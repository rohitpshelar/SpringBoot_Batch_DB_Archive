# Archive Batch (Spring Boot)

This project moves 2 records every 2 minutes from `main_db.my_table` to `archive_db.my_table`.
It inserts into the archive DB and, after verifying inserts succeeded, deletes originals from the main DB.
Database: MySQL

Run with Gradle (requires Gradle installed if `./gradlew` wrapper is not present):

```bash
# build
gradle bootJar

# run
gradle bootRun
```

Update `src/main/resources/application.yml` with your DB credentials.

Note: This zip does NOT include the Gradle wrapper. If you want a wrapper included, run `gradle wrapper` locally and re-package, or ask me to include it and I will attempt to add one (may be large).