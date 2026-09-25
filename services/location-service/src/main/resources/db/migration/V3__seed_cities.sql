-- IDs are explicit on purpose: flight-ops-service and airline-core-service store these IDs
-- as logical references, so the rows must keep the IDs they had before Flyway was introduced.
INSERT INTO cities (id, city_code, country_code, country_name, name, region_code, time_zone_id) VALUES
    (5,  'LOS', 'NG', 'Nigeria',              'Lagos',     'LA',  'Africa/Lagos'),
    (6,  'ABV', 'NG', 'Nigeria',              'Abuja',     'FC',  'Africa/Lagos'),
    (7,  'LON', 'GB', 'United Kingdom',       'London',    'ENG', 'Europe/London'),
    (8,  'NYC', 'US', 'United States',        'New York',  'NY',  'America/New_York'),
    (9,  'DXB', 'AE', 'United Arab Emirates', 'Dubai',     'DU',  'Asia/Dubai'),
    (10, 'PAR', 'FR', 'France',               'Paris',     'IDF', 'Europe/Paris'),
    (11, 'SIN', 'SG', 'Singapore',            'Singapore', 'SG',  'Asia/Singapore'),
    -- Delhi and Mumbai did not exist as cities before: their airports (DEL, BOM) were wrongly attached to Dubai.
    (12, 'DEL', 'IN', 'India',                'Delhi',     'DL',  'Asia/Kolkata'),
    (13, 'BOM', 'IN', 'India',                'Mumbai',    'MH',  'Asia/Kolkata');
