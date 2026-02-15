import { Injectable } from "@nestjs/common";
import axios, { AxiosError } from "axios";
import { EvidenceEventDto } from "./evidence.dto";

@Injectable()
export class LedgerClient {
  private baseUrl = process.env.LEDGER_URL ?? "http://localhost:8081";

  async writeEvent(event: EvidenceEventDto): Promise<void> {
    try {
      await axios.post(`${this.baseUrl}/internal/ledger/events`, event, {
        headers: { "Content-Type": "application/json" },
        timeout: 5000 // fail fast so we can return a clear error
      });
    } catch (err: any) {
      // Re-throw as AxiosError-compatible so controller can map correctly
      throw err;
    }
  }

  async health(): Promise<boolean> {
    try {
      const res = await axios.get(`${this.baseUrl}/health`, { timeout: 2000 });
      return res.status === 200;
    } catch {
      return false;
    }
  }
}
