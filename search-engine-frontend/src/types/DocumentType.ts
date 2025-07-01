export const DocumentTypeEnum = {
  WEB_PAGE: "web_page",
  PDF: "pdf",
  IMAGE: "image",
  VIDEO: "video",
  UNKNOWN: "unknown",
} as const;

// Type dérivé des valeurs
export type DocumentType = (typeof DocumentTypeEnum)[keyof typeof DocumentTypeEnum];
