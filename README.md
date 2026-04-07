# NPCI 3DS ACS — Local RAG Q&A

A small **retrieval-augmented generation (RAG)** demo in **Java** that answers questions about the payments product using your own document corpus. Everything runs **locally**: embeddings and the chat model are served by [Ollama](https://ollama.com/), so no external API keys are required.

## What it does

1. **Ingest** — Reads `.txt` and `.pdf` files from a `docs/` folder, extracts text (PDFs via Apache PDFBox), and splits content into fixed-size chunks (~500 characters).
2. **Embed** — Each chunk is sent to Ollama’s embedding API (`nomic-embed-text`) and stored as a dense vector.
3. **Index** — Vectors are indexed with an **HNSW** graph ([hnswlib-core](https://github.com/jelmerk/hnswlib)) using **cosine distance**. Chunk text is kept in a side map and persisted with the index.
4. **Query** — Your question is embedded, the **top 3** similar chunks are retrieved, and they are passed as context to a local LLM (**Mistral** via Ollama) with a strict “answer only from context” style prompt.

The app prints an embedding dimension sanity check, builds (or rebuilds) the index, saves `vector.index` + `text.store`, then starts an interactive “Ask:” loop in the terminal.

## Requirements

| Component | Notes |
|-----------|--------|
| **JDK** | Java compatible with the Gradle setup in this repo (see `build.gradle`). |
| **Gradle** | Use the included wrapper: `./gradlew` (no global Gradle install required). |
| **[Ollama](https://ollama.com/)** | Must be running locally (`http://localhost:11434`). |
| **Models** | Pull the same models the code expects (see [Configuration](#configuration)). |

### Install and pull Ollama models

```bash
ollama pull mistral
ollama pull nomic-embed-text
```

Ensure Ollama is running before you start the application.

## Project layout

```
npcirag/
├── docs/                 # Place your NPCI 3DS ACS (.txt / .pdf) documents here
├── src/main/java/com/npcirag/code/
│   ├── Main.java         # Entry: load docs, chunk, embed, index, REPL
│   ├── EmbeddingClient.java   # Ollama /api/embeddings
│   ├── OllamaClient.java      # Ollama /api/generate
│   ├── VectorStore.java       # HNSW index + serialized text map
│   ├── VectorItem.java
│   ├── TextChunker.java
│   └── DocumentLoader.java    # Alternate loader (not used by Main today)
├── build.gradle
└── README.md
```

Create the `docs/` directory if it does not exist and add your corpus there.

## Build and run

```bash
cd npcirag
./gradlew run
```

On Windows:

```bash
gradlew.bat run
```

**First run:** indexing may take a while if you have many or large PDFs, because each chunk triggers an embedding request to Ollama.

## Configuration

- **Embedding model** — `EmbeddingClient.java`: `nomic-embed-text` (must match `ollama pull`).
- **Chat model** — `OllamaClient.java`: `mistral`.
- **Vector dimension** — `Main.java` constructs `new VectorStore(768)`, which matches typical output size for `nomic-embed-text`. If you switch embedding models, update this dimension to match the new embedding length.
- **Chunking** — `TextChunker.chunk(doc, 500)` in `Main.java`; adjust chunk size for recall vs. context size.
- **Top-k retrieval** — `store.search(queryVector, 3)` in `Main.java`.

URLs are hardcoded to `http://localhost:11434`. To use another host, change `EmbeddingClient` and `OllamaClient`.

## Generated files

After a run, the workspace may contain:

- `vector.index` — HNSW index (binary).
- `text.store` — Serialized map of chunk id → text (binary).

These are listed in `.gitignore` so they are not committed by mistake. Delete them if you want a full re-index from scratch.

## Current behavior notes

- **Full re-index on every startup** — `Main` always loads `docs/`, re-chunks, re-embeds, and overwrites the saved index. To skip re-indexing after the first run, you would need to add a branch that calls `VectorStore.load(...)` when the index files already exist.
- **`DocumentLoader`** — Walks a folder tree for plain text files; `Main` uses its own `loadDocs` with `.txt` and `.pdf` support instead.

- ## Disclaimer

This repository is a **personal technical demo**. **NPCI**, **3DS**, and **ACS** are subject to their owners’ trademarks and policies. Only include documents you are allowed to store and process. Do not commit confidential or proprietary PDFs to a public GitHub repo unless permitted.

## License

Specify your preferred license in a `LICENSE` file when you publish the repository (this repo does not include one by default).
