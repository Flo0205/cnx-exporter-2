# cnx-exporter-2

**cnx-exporter-2** is a Java command-line application for exporting communities from a CNX instance.

It connects to a configured CNX host, authenticates with user credentials, retrieves a community (and sub-communities) by its ID, and exports it to a specified directory.

It is possible to export the community as one JSON file or multiple small JSON files.

Files will be downloaded into a folder structure, representing the original files/forum/wiki structure.

---

## Requirements

* Java 17
* Maven installed
* Network access to the CNX host
* Valid CNX credentials

---

## Build

To build the project using Maven:

```bash
mvn clean package
```

This will generate a `.jar` file in the `target/` directory.

---

## Usage

Run the application using:

```bash
java -jar cnx-exporter-2.0.0.jar <communityId> [OPTIONS]
```

---

## Parameters

| Parameter         | Required | Description                                  |
|-------------------|----------|----------------------------------------------|
| `communityId`     | ✅ Yes    | ID of the community to export                |
| `--host`          | ❌ No*    | CNX host URL                                 |
| `-u` `--user`     | ❌ No*    | CNX username                                 |
| `-p` `--password` | ❌ No*    | CNX password                                 |
| `--out-dir`       | ❌ No     | Output directory for exported files          |
| `-s` `--single`   | ❌ No     | Export as single file instead of split files |

* If not provided as arguments, these values must be set via environment variables.

---

## Environment Variables

You can configure connection settings via environment variables instead of passing them as arguments:

```bash
export CNX_HOST=https://example.org
export CNX_USER=myuser
export CNX_PASS=secret
export CNX_OUTDIR=./export
```

Then run:

```bash
java -jar cnx-exporter-2.0.0.jar <communityId>
```

---

## Examples

### Example 1: Using environment variables

```bash
export CNX_HOST=https://example.org
export CNX_USER=myuser
export CNX_PASS=secret
export CNX_OUTDIR=./export

java -jar cnx-exporter-2.0.0.jar fd123c97-bee4-48e2-851c-4659a20cf779
```

### Example 2: Passing all parameters directly

```bash
java -jar cnx-exporter-2.0.0.jar \
fd123c97-bee4-48e2-851c-4659a20cf779 \
--host=https://example.org \
--user=myuser \
--password=secret \
--output-dir=export
```

### Example 3: Export as single JSON

```bash
java -jar cnx-exporter-2.0.0.jar \
fd123c97-bee4-48e2-851c-4659a20cf779 \
--host=https://example.org \
--user=myuser \
--password=secret \
--output-dir=export \
--single
```

### Example 4: Using interactive password input

```bash
java -jar cnx-exporter-2.0.0.jar \
fd123c97-bee4-48e2-851c-4659a20cf779 \
--host=https://example.org \
-u myuser \
-p
```

---

## Notes

* Command-line arguments take precedence over environment variables.
* `outDir` is optional. If not set, a default directory (`export`) will be used.
* Required dependencies are in `target/lib/`

---

## Disclaimer

Parts of this project were developed with the assistance of AI-based tools.  
All generated content was reviewed and adapted to meet the project's requirements.

---

## Dependencies

This project uses:
- Jackson Databind, Core, and Annotations (Apache 2.0)
- Picocli (Apache 2.0)
- Lombok (MIT)

See their respective repositories for license details.
