package com.testforge;

import org.testcontainers.utility.DockerImageName;

final class PostgreSqlTestImage {
  static final DockerImageName POSTGRES =
      DockerImageName.parse(
              "postgres:18.4-trixie@sha256:d129b9577d274bb96cbd44d902bdeb1b935c89247d161241e9154cba64e13df4")
          .asCompatibleSubstituteFor("postgres");

  /** Prevents instantiation of the shared test image descriptor. */
  private PostgreSqlTestImage() {}
}
