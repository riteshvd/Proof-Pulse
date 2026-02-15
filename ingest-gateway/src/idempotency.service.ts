import { BadRequestException, Injectable } from "@nestjs/common";
import Redis from "ioredis";

@Injectable()
export class IdempotencyService {
  private redis: Redis;

  constructor() {
    // local dev defaults (Phase 6 will make this configurable)
    this.redis = new Redis(process.env.REDIS_URL ?? "redis://localhost:6379");
  }

  getKeyFromHeader(headerValue: any): string {
    const key = (headerValue ?? "").toString().trim();
    if (!key) throw new BadRequestException("Idempotency-Key required");
    return `idem:${key}`;
  }

  async getCachedResponse(key: string): Promise<any | null> {
    const raw = await this.redis.get(key);
    return raw ? JSON.parse(raw) : null;
  }

  async setCachedResponse(key: string, value: any, ttlSeconds = 300): Promise<void> {
    await this.redis.set(key, JSON.stringify(value), "EX", ttlSeconds);
  }
}
