-- city_id is resolved per row from the city's stable city_code (set in V3), not a hardcoded literal —
-- no source-controlled data hardcodes airport ids either, now that ids are generated UUIDs.
INSERT INTO airports (id, iata_code, name, street, postal_code, latitude, longitude, time_zone_id, city_id)
SELECT UUID(), a.iata_code, a.name, a.street, a.postal_code, a.latitude, a.longitude, a.time_zone_id, c.id
FROM (
    SELECT 'LOS' AS iata_code, 'Murtala Muhammed International Airport'            AS name, 'Ikeja'                            AS street, '100271'  AS postal_code, 6.5774  AS latitude, 3.3212   AS longitude, 'Africa/Lagos'     AS time_zone_id, 'LOS' AS city_code UNION ALL
    SELECT 'QOW', 'Sam Mbakwe Cargo Airport',                          'Cargo Terminal Road',              '100275',  6.6102,  3.2901,   'Africa/Lagos',     'LOS' UNION ALL
    SELECT 'ABV', 'Nnamdi Azikiwe International Airport',              'Airport Road',                     '900107',  9.0068,  7.2632,   'Africa/Lagos',     'ABV' UNION ALL
    SELECT 'ABX', 'Abuja Executive Airport',                           'Executive Aviation Road',          '900108',  8.9981,  7.2514,   'Africa/Lagos',     'ABV' UNION ALL
    SELECT 'LHR', 'London Heathrow Airport',                           'Longford TW6',                     'TW6',     51.47,   -0.4543,  'Europe/London',    'LON' UNION ALL
    SELECT 'LGW', 'London Gatwick Airport',                            'Horley',                           'RH6 0NP', 51.1537, -0.1821,  'Europe/London',    'LON' UNION ALL
    SELECT 'LGA', 'LaGuardia Airport',                                 '94-00 Grand Central Pkwy',         '11371',   40.7769, -73.874,  'America/New_York', 'NYC' UNION ALL
    SELECT 'DXB', 'Dubai International Airport',                       'Airport Road',                     '2525',    25.2532, 55.3657,  'Asia/Dubai',       'DXB' UNION ALL
    SELECT 'DWC', 'Al Maktoum International Airport',                  'Dubai South',                      '644828',  24.8964, 55.1614,  'Asia/Dubai',       'DXB' UNION ALL
    SELECT 'JFK', 'John F. Kennedy International Airport',             'JFK Access Road',                  '11430',   40.6413, -73.7781, 'America/New_York', 'NYC' UNION ALL
    SELECT 'DEL', 'Indira Gandhi International Airport',               'New Udan Bhawan, IGI Airport',     '110037',  28.5562, 77.1,     'Asia/Kolkata',     'DEL' UNION ALL
    SELECT 'BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Sahar, Andheri East',              '400099',  19.0887, 72.8679,  'Asia/Kolkata',     'BOM' UNION ALL
    SELECT 'CDG', 'Paris Charles de Gaulle Airport',                   '1 Rue de Paris, Roissy-en-France', '95700',   49.0097, 2.5479,   'Europe/Paris',     'PAR' UNION ALL
    SELECT 'ORY', 'Paris Orly Airport',                                '94390 Orly',                       '94390',   48.7233, 2.3794,   'Europe/Paris',     'PAR' UNION ALL
    SELECT 'SIN', 'Singapore Changi Airport',                          'Airport Boulevard, Changi',        '819643',  1.3644,  103.9915, 'Asia/Singapore',   'SIN'
) a
JOIN cities c ON c.city_code = a.city_code;
