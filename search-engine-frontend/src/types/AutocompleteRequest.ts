export interface AutocompleteRequest {
  prefix: string;
  sessionId?: string;
  language?: string;
  maxSuggestions?: number;
}