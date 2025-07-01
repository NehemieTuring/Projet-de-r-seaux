-- Insert initial sources
INSERT INTO sources (url, name, description, created_at) VALUES
('https://example.com', 'Example Site', 'A sample website for testing', NOW()),
('https://news.com', 'News Portal', 'A news aggregation site', NOW())
ON CONFLICT (url) DO NOTHING;

-- Insert initial crawl sessions
INSERT INTO crawl_sessions (source_id, session_id, started_at, status) VALUES
((SELECT id FROM sources WHERE url = 'https://example.com'), 'session-001', NOW(), 'RUNNING'),
((SELECT id FROM sources WHERE url = 'https://news.com'), 'session-002', NOW(), 'RUNNING')
ON CONFLICT (session_id) DO NOTHING;