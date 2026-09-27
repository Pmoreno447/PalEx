INSERT INTO users (provider_id, email, role, name, username, phone) VALUES ('pedro', 'p@p.com', 'ADMIN', 'Pedro Moreno', 'pmoreno447', '+34640314324');

INSERT INTO courts (name, price, active, slot_minutes, open_time, close_time) VALUES ('Pista 1', 12.00, TRUE, 90, '09:00:00', '23:00:00');
INSERT INTO courts (name, price, active, slot_minutes, open_time, close_time) VALUES ('Pista 2', 10.50, FALSE, 60, '10:00:00', '22:00:00');

-- Reservas de la Pista 1 para el 29/09/2026 (turnos de 90 min desde las 09:00)
-- 12:00-13:30 ocupa un turno exacto de la rejilla
INSERT INTO bookings (court_id, init_date_time, end_date_time, amount, state)
VALUES ((SELECT id FROM courts WHERE name = 'Pista 1'), '2026-09-29 12:00:00', '2026-09-29 13:30:00', 12.00, 'PENDING');

-- 16:30-18:00, otro turno exacto
INSERT INTO bookings (court_id, init_date_time, end_date_time, amount, state)
VALUES ((SELECT id FROM courts WHERE name = 'Pista 1'), '2026-09-29 16:30:00', '2026-09-29 18:00:00', 12.00, 'COMPLETED');

-- 19:30-22:30: dura 180 min, así que pisa dos turnos de la rejilla (19:30 y 21:00)
INSERT INTO bookings (court_id, init_date_time, end_date_time, amount, state)
VALUES ((SELECT id FROM courts WHERE name = 'Pista 1'), '2026-09-29 19:30:00', '2026-09-29 22:30:00', 24.00, 'PENDING');
