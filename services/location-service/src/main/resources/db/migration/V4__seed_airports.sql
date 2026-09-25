-- IDs are explicit on purpose: flight-ops-service stores airport IDs as logical references
-- (departure/arrival), so every airport keeps the ID it had before Flyway was introduced.
-- The only change to existing rows: DEL (12) and BOM (13) now point at their own cities instead of Dubai (9).
INSERT INTO airports (id, iata_code, name, street, postal_code, latitude, longitude, time_zone_id, city_id) VALUES
    (1,  'LOS', 'Murtala Muhammed International Airport',            'Ikeja',                            '100271',  6.5774,  3.3212,   'Africa/Lagos',     5),
    (2,  'QOW', 'Sam Mbakwe Cargo Airport',                          'Cargo Terminal Road',              '100275',  6.6102,  3.2901,   'Africa/Lagos',     5),
    (3,  'ABV', 'Nnamdi Azikiwe International Airport',              'Airport Road',                     '900107',  9.0068,  7.2632,   'Africa/Lagos',     6),
    (4,  'ABX', 'Abuja Executive Airport',                           'Executive Aviation Road',          '900108',  8.9981,  7.2514,   'Africa/Lagos',     6),
    (5,  'LHR', 'London Heathrow Airport',                           'Longford TW6',                     'TW6',     51.47,   -0.4543,  'Europe/London',    7),
    (6,  'LGW', 'London Gatwick Airport',                            'Horley',                           'RH6 0NP', 51.1537, -0.1821,  'Europe/London',    7),
    (8,  'LGA', 'LaGuardia Airport',                                 '94-00 Grand Central Pkwy',         '11371',   40.7769, -73.874,  'America/New_York', 8),
    (9,  'DXB', 'Dubai International Airport',                       'Airport Road',                     '2525',    25.2532, 55.3657,  'Asia/Dubai',       9),
    (10, 'DWC', 'Al Maktoum International Airport',                  'Dubai South',                      '644828',  24.8964, 55.1614,  'Asia/Dubai',       9),
    (11, 'JFK', 'John F. Kennedy International Airport',             'JFK Access Road',                  '11430',   40.6413, -73.7781, 'America/New_York', 8),
    (12, 'DEL', 'Indira Gandhi International Airport',               'New Udan Bhawan, IGI Airport',     '110037',  28.5562, 77.1,     'Asia/Kolkata',     12),
    (13, 'BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Sahar, Andheri East',              '400099',  19.0887, 72.8679,  'Asia/Kolkata',     13),
    (14, 'CDG', 'Paris Charles de Gaulle Airport',                   '1 Rue de Paris, Roissy-en-France', '95700',   49.0097, 2.5479,   'Europe/Paris',     10),
    (15, 'ORY', 'Paris Orly Airport',                                '94390 Orly',                       '94390',   48.7233, 2.3794,   'Europe/Paris',     10),
    (16, 'SIN', 'Singapore Changi Airport',                          'Airport Boulevard, Changi',        '819643',  1.3644,  103.9915, 'Asia/Singapore',   11);
