import {
  Body,
  Controller,
  Headers,
  HttpCode,
  Post,
  ConflictException,
  ServiceUnavailableException,
  BadRequestException
} from "@nestjs/common";
import { ApiHeader, ApiOperation, ApiResponse, ApiTags } from "@nestjs/swagger";
import { EvidenceEventDto } from "./evidence.dto";
import { IdempotencyService } from "./idempotency.service";
import { LedgerClient } from "./ledger.client";

@ApiTags("events")
@Controller()
export class EventsController {
  constructor(
    private readonly idem: IdempotencyService,
    private readonly ledger: LedgerClient
  ) {}

  @Post("/events")
  @HttpCode(201)
  @ApiHeader({ name: "Idempotency-Key", required: true })
  @ApiOperation({ summary: "Ingest a new evidence event (idempotent)" })
  @ApiResponse({ status: 201, description: "Accepted" })
  @ApiResponse({ status: 409, description: "Duplicate eventId" })
  @ApiResponse({ status: 503, description: "Ledger unavailable" })
  async ingest(
    @Headers("idempotency-key") idemHeader: string,
    @Body() body: EvidenceEventDto
  ) {
    const key = this.idem.getKeyFromHeader(idemHeader);

    const cached = await this.idem.getCachedResponse(key);
    if (cached) return cached;

    try {
      await this.ledger.writeEvent(body);
    } catch (e: any) {
      const status = e?.response?.status;
      const data = e?.response?.data;

      // Common ledger responses
      if (status === 409) throw new ConflictException("Duplicate eventId");
      if (status === 400) throw new BadRequestException(data?.error ?? data ?? "Ledger rejected request");

      // If ledger returned something else (404/500), expose it clearly
      if (status) {
        throw new ServiceUnavailableException(
          `Ledger returned HTTP ${status}: ${typeof data === "string" ? data : JSON.stringify(data)}`
        );
      }

      // No HTTP response at all => unreachable/down
      const ledgerUp = await this.ledger.health();
      if (!ledgerUp) {
        throw new ServiceUnavailableException(
          "Ledger service unavailable (check http://localhost:8081/health)"
        );
      }

      throw new ServiceUnavailableException(`Upstream ledger error: ${e?.message ?? "unknown"}`);
    }

    const response = { eventId: body.eventId, status: "ACCEPTED" };
    await this.idem.setCachedResponse(key, response);
    return response;
  }
}
