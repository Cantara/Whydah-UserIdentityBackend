-- Normalize inconsistent placeholder names to standard values:
-- Firstname, Lastname, placeholder@example.com

UPDATE UserIdentity SET firstname = 'Firstname'
WHERE firstname IN ('Fornavn', 'firstname', 'User');

UPDATE UserIdentity SET lastname = 'Lastname'
WHERE lastname IN ('Etternavn', 'lastname', 'LastNamePlaceholder');
