-- Package catalogue and the five agents of the LineBook demo. Their ids are the ones customers refer to.
INSERT INTO package_plans (id, name, amount, sort_order) VALUES
    ('pkg-basic',   'Basic FTA',  199.00, 1),
    ('pkg-family',  'Family',     299.00, 2),
    ('pkg-hd',      'HD Family',  449.00, 3),
    ('pkg-sports',  'Sports HD',  599.00, 4),
    ('pkg-premium', 'Premium 4K', 799.00, 5);

INSERT INTO agents (id, name, phone, area, active) VALUES
    ('ag-01', 'Ramesh Yadav',  '9849011201', 'Ameerpet',     TRUE),
    ('ag-02', 'S. Padma',      '9866123344', 'Kukatpally',   TRUE),
    ('ag-03', 'Abdul Rahman',  '9700441188', 'Mehdipatnam',  TRUE),
    ('ag-04', 'Venkat Goud',   '9988772211', 'LB Nagar',     TRUE),
    ('ag-05', 'Kiran Kumar',   '9123456709', 'Secunderabad', TRUE);
