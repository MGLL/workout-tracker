-- Starter catalog. Reference data for a shared library, so it is versioned here
-- rather than kept as a test fixture. UUIDs are fixed so they are the same
-- in every environment.

INSERT INTO exercise (id, code, muscle_group, body_part, equipment) VALUES
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e01', 'bench-press',       'CHEST',      'UPPER_BODY', 'BARBELL'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e02', 'romanian-deadlift', 'HAMSTRINGS', 'LOWER_BODY', 'BARBELL'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e03', 'biceps-curl',       'BICEPS',     'UPPER_BODY', 'DUMBBELL'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e04', 'triceps-extension', 'TRICEPS',    'UPPER_BODY', 'CABLE');

INSERT INTO exercise_translation (exercise_id, locale, name, description) VALUES
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e01', 'en', 'Bench press',
     'Lie on a flat bench and press the barbell from chest level to full arm extension.'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e01', 'fr', 'Développé couché',
     'Allongé sur un banc plat, pousser la barre depuis la poitrine jusqu''à l''extension complète des bras.'),

    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e02', 'en', 'Romanian deadlift',
     'Hinge at the hips with a slight knee bend, lowering the barbell along the legs to stretch the hamstrings.'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e02', 'fr', 'Soulevé de terre roumain',
     'Fléchir les hanches en gardant les genoux légèrement pliés, en descendant la barre le long des jambes pour étirer les ischio-jambiers.'),

    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e03', 'en', 'Biceps curl',
     'Curl the dumbbells from a full stretch to the shoulders, keeping the elbows pinned to the torso.'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e03', 'fr', 'Curl biceps',
     'Remonter les haltères jusqu''aux épaules en gardant les coudes collés au buste.'),

    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e04', 'en', 'Triceps extension',
     'Extend the elbows against the cable until the arms are straight, keeping the upper arms still.'),
    ('0f9b1a4e-1c2d-4b3a-8e5f-6a7b8c9d0e04', 'fr', 'Extension triceps',
     'Tendre les coudes contre la poulie jusqu''à ce que les bras soient droits, en gardant les bras immobiles.');
