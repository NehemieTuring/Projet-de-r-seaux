export interface ComponentStatus {
  status: string;
  errorMessage?: string | null;
}

export interface HealthResponse {
  status: string;
  components: Record<string, ComponentStatus>;
}