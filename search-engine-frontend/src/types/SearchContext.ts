export interface GeoLocation {
  country?: string;
  city?: string;
  latitude?: number;
  longitude?: number;
}

export interface UserProfile {
  deviceType?: string;
  preferredLanguage?: string;
}

export interface SearchContext {
  sessionId?: string;
  geoLocation?: GeoLocation;
  userProfile?: UserProfile;
  createdAt?: string; // LocalDateTime is serialized as ISO string
  updatedAt?: string; // LocalDateTime is serialized as ISO string
}