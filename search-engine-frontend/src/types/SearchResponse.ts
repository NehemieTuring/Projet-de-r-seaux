import type { SearchContext } from './SearchContext';

export interface DocumentResponse {
  id: string;
  url: string;
  title: string;
  snippet?: string;
  score?: number;
}

export interface SearchResponse {
  results: DocumentResponse[];
  totalResults: number;
  context?: SearchContext;
}