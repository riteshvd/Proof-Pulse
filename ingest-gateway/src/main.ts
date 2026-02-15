import "reflect-metadata";
import { ValidationPipe } from "@nestjs/common";
import { NestFactory } from "@nestjs/core";
import { DocumentBuilder, SwaggerModule } from "@nestjs/swagger";
import { AppModule } from "./app.module";

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true
    })
  );

  const config = new DocumentBuilder()
    .setTitle("ProofPulse Ingestion Gateway")
    .setVersion("0.1.0")
    .build();

  const doc = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup("/docs", app, doc);

  await app.listen(3001);
}
bootstrap();
