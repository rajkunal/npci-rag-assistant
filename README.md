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

## Fine-tuning and improving quality

In this project, “fine-tuning” usually means **tuning the RAG pipeline** (data, chunking, retrieval, and prompts), not training new model weights. The code does not perform gradient-based fine-tuning; you would do that outside the app (e.g. with Ollama custom models, LoRA tooling, or a separate training stack) if you need a domain-specific LLM or embedder.

### Tune retrieval (fastest wins)

| Lever | Where | What to try |
|--------|--------|-------------|
| **Chunk size** | `TextChunker.chunk(doc, 500)` in `Main.java` | Smaller chunks (~300–400) can sharpen answers when facts are dense; larger chunks (~700–1000) help when answers need surrounding paragraphs. PDFs with tables/lists may need experimentation. |
| **Chunk overlap** | Not implemented today | Adding overlap (e.g. 50–100 characters) between consecutive chunks reduces cases where a boundary cuts a sentence in half. Extend `TextChunker` or the loop in `Main` to step by `chunkSize - overlap`. |
| **Top-k** | `store.search(queryVector, 3)` | Increase `k` (e.g. 5–10) when the model misses relevant passages; decrease when the prompt gets noisy or the model copies unrelated snippets. |
| **HNSW index** | `VectorStore.java` (`withM`, `withEf`) | Higher `M` / `ef` can improve recall at the cost of build time and memory; defaults are a reasonable starting point for small corpora. |
| **Embedding model** | `EmbeddingClient.java` | Swapping to another Ollama embedding model may help domain terminology; always update `VectorStore` **dimension** in `Main.java` and rebuild the index. |

### Tune generation

| Lever | Where | What to try |
|--------|--------|-------------|
| **System-style instruction** | Prompt string in `Main.java` (`Answer only from context...`) | Add rules: cite section names, respond “not in context” when unsure, bullet format for procedures, or NPCI-specific terminology glossaries in the prompt. |
| **Chat model** | `OllamaClient.java` (`mistral`) | Stronger or more instruction-tuned models (whatever you `ollama pull`) often follow constraints better; keep embedding and chat models independent. |
| **Ollama parameters** | `OllamaClient` JSON body | You can extend the request with options such as `temperature` (lower for factual Q&A), `num_ctx`, or `num_predict` if you need longer answers—see [Ollama’s generate API](https://github.com/ollama/ollama/blob/main/docs/api.md). |

### Tune data and workflow

- **Corpus** — Add missing manuals, normalize duplicates, and prefer clean `.txt` exports when PDF extraction is noisy.
- **Incremental index** — Loading a saved index when files have not changed avoids stale vectors and speeds iteration while you only change prompts or `k`.
- **Evaluation** — Keep a small list of real questions and expected themes; change one knob at a time so retrieval vs. generation issues stay separable.

### True model fine-tuning (optional)

If you need **weight-level** adaptation (e.g. issuer-specific phrasing beyond RAG), explore training or adapters for your chosen stack, then **serve that model through Ollama** and point `OllamaClient` / `EmbeddingClient` at the new model name. The Java RAG shell stays the same; you rebuild the vector index if the embedding model changes.

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
