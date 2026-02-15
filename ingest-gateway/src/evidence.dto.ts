import { ApiProperty } from "@nestjs/swagger";
import {
  IsIn,
  IsISO8601,
  IsInt,
  IsObject,
  IsString,
  IsUUID,
  Matches,
  Max,
  MaxLength,
  Min,
  MinLength
} from "class-validator";

export class EvidenceEventDto {
  @ApiProperty({ enum: [1] })
  @IsInt()
  @Min(1)
  @Max(1)
  @IsIn([1])
  schemaVersion!: number;

  @ApiProperty({ format: "uuid" })
  @IsUUID()
  eventId!: string;

  @ApiProperty({ example: "proofpulse-core" })
  @IsString()
  @MinLength(3)
  @MaxLength(64)
  @Matches(/^[a-zA-Z0-9._-]+$/)
  projectId!: string;

  @ApiProperty({ example: "repo:proofpulse/service:ledger" })
  @IsString()
  @MinLength(3)
  @MaxLength(128)
  @Matches(/^[a-zA-Z0-9./:_-]+$/)
  artifactId!: string;

  @ApiProperty()
  @IsString()
  @MinLength(1)
  source!: string;

  @ApiProperty({ format: "date-time" })
  @IsISO8601()
  timestamp!: string;

  @ApiProperty()
  @IsString()
  @MinLength(1)
  type!: string;

  @ApiProperty({ type: "object" })
  @IsObject()
  payload!: Record<string, any>;
}
