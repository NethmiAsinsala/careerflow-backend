# Local AI resume analysis

## Run and test

1. Start Ollama locally. If it is not running, use `ollama serve` in a separate terminal. The integration uses http://127.0.0.1:11434.
2. Check `ollama list`. The default model is `qwen2.5:1.5b`, which must finish downloading before live analysis works. To use another installed local model, add `OLLAMA_MODEL=your-model-name` to Main's IntelliJ environment variables.
3. Reload the Maven project in IntelliJ to resolve the new Apache PDFBox dependency, then restart Main. Keep JWT_SECRET configured.
4. Log in as a JOB_SEEKER in Swagger and authorize.
5. Upload a text-based PDF with POST /api/job-seekers/me/resume.
6. Call POST /api/ai/resume/analyze with no request body. Analysis can take time on a CPU.

The response includes provider, model, resumeSha256, page count, feedback (summary, skills, strengths, improvements), and a review reminder. The hash identifies the uploaded file used for that analysis. If the resume changes during analysis, this result still refers to the original file.

## Behavior

- Only candidates can analyze their own uploaded resume. IDs supplied in query parameters cannot choose another candidate.
- Text is sent to the local Ollama service. No hosted API key is needed. Configuration accepts loopback IP addresses only and rejects cloud-labelled model names. Use a genuinely local installed model, not a custom proxy to a hosted service.
- AI output does not modify skills or profile fields, make hiring decisions, or replace the existing deterministic job-match percentage.
- Feedback is generated on demand, returned without caching, and not stored as history.
- PDF limits: 5 MiB, 20 pages, and 30,000 extracted characters. Encrypted, malformed, or image-only PDFs cannot be analyzed. OCR is not included.
- PDF parsing uses Apache PDFBox. Resume text is treated as document content in the model prompt, not as instructions. Generated output is validated for expected fields, types, and lengths. Render feedback as plain text in the frontend.
- One analysis runs at a time per backend instance. Ollama requests time out after 120 seconds by default. Set OLLAMA_TIMEOUT_SECONDS between 1 and 300 if needed.

## Errors

| Status | Meaning |
| --- | --- |
| 401 / 403 | Login required / candidate role required |
| 404 | No uploaded resume |
| 422 | PDF cannot be extracted or exceeds analysis limits |
| 429 | Another analysis is in progress; retry later |
| 502 | Model returned incomplete or invalid structured feedback |
| 503 | Ollama unavailable, model missing, or model execution failed |
| 504 | Model exceeded the configured timeout |

If Ollama cannot load the model, check its logs and available memory. Close unneeded applications or configure a smaller locally installed model. Slow or unavailable AI does not prevent the backend from starting or other APIs from working.

Suggested commit: `feat: add local Ollama resume analysis with PDF extraction`

Implementation references: [Ollama chat API](https://docs.ollama.com/api/chat), [PDFBox 3 migration and loading API](https://pdfbox.apache.org/3.0/migration.html).

Download the default text model with `ollama pull qwen2.5:1.5b`. The previously installed qwen3-vl:4b returned feedback in the thinking field instead of the answer; that output is rejected rather than exposed.
