import axios from 'axios';
import type { SearchRequest } from '../types/SearchRequest';
import type { SearchResponse } from '../types/SearchResponse';
import type { AutocompleteRequest } from '../types/AutocompleteRequest';
import type { AutocompleteResponse } from '../types/AutocompleteResponse';
import type { HealthResponse } from '../types/HealthResponse';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

export const search = async (request: SearchRequest): Promise<SearchResponse> => {
  const response = await api.post('/api/search', request);
  return response.data;
};

export const getAutocompleteSuggestions = async (request: AutocompleteRequest): Promise<AutocompleteResponse> => {
  const response = await api.post('/api/autocomplete', request);
  return response.data;
};

export const checkHealth = async (): Promise<HealthResponse> => {
  const response = await api.get('/api/health');
  return response.data;
};