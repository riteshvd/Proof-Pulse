import { Module } from "@nestjs/common";
import { HealthController } from "./health.controller";
import { EventsController } from "./events.controller";
import { IdempotencyService } from "./idempotency.service";
import { LedgerClient } from "./ledger.client";

@Module({
  controllers: [HealthController, EventsController],
  providers: [IdempotencyService, LedgerClient]
})
export class AppModule {}
