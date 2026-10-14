-- city_code is the stable key other seed rows (V4) join on; the id itself is now a generated UUID,
-- not a cross-service-referenced literal (no source-controlled data hardcodes these ids anymore).
INSERT INTO cities (id, city_code, country_code, country_name, name, region_code, time_zone_id) VALUES
    (UUID(), 'LOS', 'NG', 'Nigeria',              'Lagos',     'LA',  'Africa/Lagos'),
    (UUID(), 'ABV', 'NG', 'Nigeria',              'Abuja',     'FC',  'Africa/Lagos'),
    (UUID(), 'PHC', 'NG', 'Nigeria',              'Port Harcourt', 'RI', 'Africa/Lagos'),
    (UUID(), 'QOW', 'NG', 'Nigeria',              'Owerri',    'IM',  'Africa/Lagos'),
    (UUID(), 'LON', 'GB', 'United Kingdom',       'London',    'ENG', 'Europe/London'),
    (UUID(), 'NYC', 'US', 'United States',        'New York',  'NY',  'America/New_York'),
    (UUID(), 'DXB', 'AE', 'United Arab Emirates', 'Dubai',     'DU',  'Asia/Dubai'),
    (UUID(), 'PAR', 'FR', 'France',               'Paris',     'IDF', 'Europe/Paris'),
    (UUID(), 'SIN', 'SG', 'Singapore',            'Singapore', 'SG',  'Asia/Singapore'),
    (UUID(), 'DEL', 'IN', 'India',                'Delhi',     'DL',  'Asia/Kolkata'),
    (UUID(), 'BOM', 'IN', 'India',                'Mumbai',    'MH',  'Asia/Kolkata');
