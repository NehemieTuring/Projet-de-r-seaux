import type { DocumentType } from "./DocumentType";

export interface SearchRequest {
  query: string;
  page?: number;
  size?: number;
  documentType?: DocumentType;
  language?: string;
  filters?: Record<string, string>;
}