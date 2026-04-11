import type { ExamplesResponseDto, ParseRequestDto, ParseResponseDto } from "../types/api";

const PLAYGROUND_API_BASE_URL =
  import.meta.env.VITE_PLAYGROUND_API_BASE_URL ?? "http://localhost:8080/sqm/playground/api/v1";

/**
 * Loads the built-in example catalog from the playground backend.
 */
export async function fetchExamples(): Promise<ExamplesResponseDto> {
  const response = await fetch(`${PLAYGROUND_API_BASE_URL}/examples`, {
    headers: {
      Accept: "application/json"
    }
  });

  if (!response.ok) {
    throw new Error(`Example request failed with status ${response.status}`);
  }

  return (await response.json()) as ExamplesResponseDto;
}

/**
 * Sends a parse request to the playground backend.
 */
export async function parseSql(request: ParseRequestDto): Promise<ParseResponseDto> {
  const response = await fetch(`${PLAYGROUND_API_BASE_URL}/parse`, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`Parse request failed with status ${response.status}`);
  }

  return (await response.json()) as ParseResponseDto;
}
