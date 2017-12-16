DROP DATABASE IF EXISTS ML3;
CREATE DATABASE ML3;
USE ML3;

CREATE TABLE Gare (
	nomGare VARCHAR(30),
    PRIMARY KEY (nomGare)
);

CREATE TABLE Ligne (
	numeroLigne INT UNSIGNED,
    PRIMARY KEY (numeroLigne)
);

CREATE TABLE Train (
	numeroTrain INT UNSIGNED,
	numeroLigne INT UNSIGNED NOT NULL,
	sensParcours ENUM('ASC', 'DESC') NOT NULL,			-- ASC = Parcours tel qu'est déclaré la ligne / DESC = Parcours dans l'autre sens
    PRIMARY KEY (numeroTrain),
    FOREIGN KEY (numeroLigne) REFERENCES Ligne(numeroLigne) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Segment (
	gareDepart VARCHAR(30),
	gareArrivee VARCHAR(30),
	longueur INT UNSIGNED NOT NULL,						-- En km
    FOREIGN KEY (gareDepart) REFERENCES Gare(nomGare) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (gareArrivee) REFERENCES Gare(nomGare) ON DELETE CASCADE ON UPDATE CASCADE,
    PRIMARY KEY (gareDepart, gareArrivee)
);

CREATE TABLE Train_Segment (
	numeroTrain INT UNSIGNED,
	gareDepart VARCHAR(30),
	gareArrivee VARCHAR(30),
	vitesse REAL UNSIGNED NOT NULL,						-- En km/h
	FOREIGN KEY (numeroTrain) REFERENCES Train(numeroTrain) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (gareDepart, gareArrivee) REFERENCES Segment(gareDepart, gareArrivee) ON DELETE CASCADE ON UPDATE CASCADE,
	PRIMARY KEY (numeroTrain, gareDepart, gareArrivee)
);

CREATE TABLE Ligne_Segment (
	numeroLigne INT UNSIGNED,
	gareDepart VARCHAR(30),
	gareArrivee VARCHAR(30),
	rang INT UNSIGNED NOT NULL,						-- Rang du segment dans la ligne (de 1 au nombre de segments de la ligne)
	FOREIGN KEY (numeroLigne) REFERENCES Ligne(numeroLigne) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (gareDepart, gareArrivee) REFERENCES Segment(gareDepart, gareArrivee) ON DELETE CASCADE ON UPDATE CASCADE,
	PRIMARY KEY (numeroLigne, gareDepart, gareArrivee)
);

CREATE TABLE Periode (
	couleurPeriode ENUM('bleue', 'blanche', 'rouge'),
    variationTarif REAL UNSIGNED NOT NULL,				-- Ex: + 10% <=> *1.1 | - 30% <=> *0.7 etc.
    PRIMARY KEY (couleurPeriode)
);

CREATE TABLE PlageDates (								-- Dates de début et fin incluses
	debut DATE,
    fin DATE NOT NULL,
	couleurPeriode ENUM('bleue', 'blanche', 'rouge'),
    PRIMARY KEY (debut),
    FOREIGN KEY (couleurPeriode) REFERENCES Periode(couleurPeriode) ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE TABLE Depart (
	horaire TIME,
	numeroTrain INT UNSIGNED,
	couleurPeriode ENUM('bleue', 'blanche', 'rouge'),
    PRIMARY KEY (horaire, numeroTrain, couleurPeriode),
    FOREIGN KEY (numeroTrain) REFERENCES Train(numeroTrain) ON DELETE CASCADE ON UPDATE CASCADE,
	FOREIGN KEY (couleurPeriode) REFERENCES Periode(couleurPeriode) ON UPDATE CASCADE
);

CREATE TABLE Classe (
	nomClasse ENUM('premiere', 'seconde', 'bar'),
    prixAuKm REAL UNSIGNED NOT NULL,
    PRIMARY KEY (nomClasse)
);

CREATE TABLE TypeVoiture (
	taille ENUM('simple', 'double'),
    nomClasse ENUM('premiere', 'seconde', 'bar'),
    numPlaceMin INT UNSIGNED NULL,
    numPlaceMax INT UNSIGNED NULL,
    PRIMARY KEY (taille, nomClasse),
    FOREIGN KEY (nomClasse) REFERENCES Classe(nomClasse) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Voiture (
	numeroVoiture INT UNSIGNED,
    numeroTrain INT UNSIGNED,
	couleurPeriode ENUM('bleue', 'blanche', 'rouge'),
    taille ENUM('simple', 'double') NOT NULL,
    nomClasse ENUM('premiere', 'seconde', 'bar') NOT NULL,
    PRIMARY KEY (numeroVoiture, numeroTrain, couleurPeriode),
    FOREIGN KEY (numeroTrain) REFERENCES Train(numeroTrain) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (couleurPeriode) REFERENCES Periode(couleurPeriode),
    FOREIGN KEY (taille, nomClasse) REFERENCES TypeVoiture(taille, nomClasse) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE Reservation (
	idReservation CHAR(6),
    mailClient VARCHAR(25) NOT NULL,
    dateHeureDepart DATETIME NOT NULL,
    prixReservation REAL UNSIGNED NOT NULL,
    gareDepart VARCHAR(30) NOT NULL,
    gareArrivee VARCHAR(30) NOT NULL,
    PRIMARY KEY (idReservation),
	FOREIGN KEY (gareDepart) REFERENCES Gare(nomGare) ON UPDATE CASCADE,
    FOREIGN KEY (gareArrivee) REFERENCES Gare(nomGare) ON UPDATE CASCADE
);

CREATE TABLE PlaceReservee (
	numeroPlace INT UNSIGNED,
	idReservation CHAR(6),
    numeroVoiture INT UNSIGNED,
    numeroTrain INT UNSIGNED,
    couleurPeriode ENUM('bleue', 'blanche', 'rouge'),
	FOREIGN KEY (idReservation) REFERENCES Reservation(idReservation),
	FOREIGN KEY (numeroVoiture, numeroTrain, couleurPeriode) REFERENCES Voiture(numeroVoiture, numeroTrain, couleurPeriode),
    PRIMARY KEY (numeroPlace, idReservation, numeroVoiture, numeroTrain, couleurPeriode)
);

-- 
-- Section 2: Database Population 
-- 

INSERT INTO Gare VALUES 
	('Paris'),
    ('Lyon'),
    ('Avignon'),
    ('Marseille'),
    ('Strasbourg'),
    ('Montpellier');
    
INSERT INTO Ligne VALUES
	(1),
    (2);

INSERT INTO Segment VALUES
	('Paris', 'Lyon', 465),							-- 465km entre Paris et Lyon
    ('Lyon', 'Avignon', 230),					
    ('Avignon', 'Marseille', 105),							
    ('Strasbourg', 'Lyon', 495),							
    ('Avignon', 'Montpellier', 95);
	
INSERT INTO Ligne_Segment VALUES
	(1, 'Paris', 'Lyon', 1),
    (1, 'Lyon', 'Avignon', 2),
    (1, 'Avignon', 'Marseille', 3),
	(2, 'Strasbourg', 'Lyon', 1),
    (2, 'Lyon', 'Avignon', 2),
    (2, 'Avignon', 'Montpellier', 3);

INSERT INTO Train VALUES
	(6607, 1, 'ASC'),								-- Car au départ de Paris
    (6650, 2, 'ASC'),			
    (6632, 1, 'DESC');								
	
INSERT INTO Train_Segment VALUES
	(6607, 'Paris', 'Lyon', 310.00),					
    (6607, 'Lyon', 'Avignon', 153.33),					
    (6607, 'Avignon', 'Marseille', 210.00),				

	(6650, 'Strasbourg', 'Lyon', 141.43),				
    (6650, 'Lyon', 'Avignon', 138.00),					
    (6650, 'Avignon', 'Montpellier', 95.00),			

	(6632, 'Marseille', 'Avignon', 157.50),				
    (6632, 'Avignon', 'Lyon', 138.00),						
    (6632, 'Lyon', 'Paris', 174.38);					
    
INSERT INTO Periode VALUES
	('bleue', 0.8),								
    ('blanche', 1),						
    ('rouge', 1.3); 				
    
INSERT INTO PlageDates VALUES
	(date '2017-10-01', date '2017-10-01', 'blanche'),
    (date '2017-10-07', date '2017-10-08', 'blanche'),
    (date '2017-10-14', date '2017-10-15', 'blanche'),
    (date '2017-10-21', date '2017-10-22', 'blanche'),
    
    (date '2017-10-28', date '2017-10-31', 'rouge'),
    
    (date '2017-10-02', date '2017-10-06', 'bleue'),			
    (date '2017-10-09', date '2017-10-13', 'bleue'),			
    (date '2017-10-16', date '2017-10-20', 'bleue'),			
    (date '2017-10-23', date '2017-10-27', 'bleue');			
    
INSERT INTO Depart VALUES
	(time '08:00:00', 6607, 'bleue'),
    (time '07:30:00', 6607, 'blanche'),
    (time '08:30:00', 6607, 'rouge'),
    
	(time '09:45:00', 6650, 'rouge'),

    (time '14:00:00', 6632, 'blanche'),
    (time '13:30:00', 6632, 'rouge');
    
INSERT INTO Classe VALUES
	('premiere', 0.295),
    ('seconde', 0.208),
    ('bar', 0.208);						

INSERT INTO TypeVoiture VALUES
	('simple', 'premiere', 11, 46),
    ('simple', 'seconde', 11, 66),
    ('double', 'premiere', 11, 70),
    ('double', 'seconde', 11, 102),
    ('simple', 'bar', 21, 36),
    ('double', 'bar', NULL, NULL);

INSERT INTO Voiture VALUES 
	(1, 6607, 'bleue', 'simple', 'premiere'),
    (2, 6607, 'bleue', 'simple', 'premiere'),
    (3, 6607, 'bleue', 'simple', 'premiere'),
    (4, 6607, 'bleue', 'simple', 'bar'),
    (5, 6607, 'bleue', 'simple', 'seconde'),
    (6, 6607, 'bleue', 'simple', 'seconde'),
    (7, 6607, 'bleue', 'simple', 'seconde'),
    (8, 6607, 'bleue', 'simple', 'seconde'),
    
	(1, 6607, 'blanche', 'double', 'premiere'),
    (2, 6607, 'blanche', 'double', 'premiere'),
    (3, 6607, 'blanche', 'double', 'premiere'),
    (4, 6607, 'blanche', 'double', 'bar'),
    (5, 6607, 'blanche', 'double', 'seconde'),
    (6, 6607, 'blanche', 'double', 'seconde'),
    (7, 6607, 'blanche', 'double', 'seconde'),
    (8, 6607, 'blanche', 'double', 'seconde'),
    
	(1, 6607, 'rouge', 'double', 'premiere'),
    (2, 6607, 'rouge', 'double', 'premiere'),
    (3, 6607, 'rouge', 'double', 'premiere'),
    (4, 6607, 'rouge', 'double', 'bar'),
    (5, 6607, 'rouge', 'double', 'seconde'),
    (6, 6607, 'rouge', 'double', 'seconde'),
    (7, 6607, 'rouge', 'double', 'seconde'),
    (8, 6607, 'rouge', 'double', 'seconde'),
	(11, 6607, 'rouge', 'double', 'premiere'),
    (12, 6607, 'rouge', 'double', 'premiere'),
    (13, 6607, 'rouge', 'double', 'premiere'),
    (14, 6607, 'rouge', 'double', 'bar'),
    (15, 6607, 'rouge', 'double', 'seconde'),
    (16, 6607, 'rouge', 'double', 'seconde'),
    (17, 6607, 'rouge', 'double', 'seconde'),
    (18, 6607, 'rouge', 'double', 'seconde'),
    
	(1, 6650, 'rouge', 'double', 'premiere'),
    (2, 6650, 'rouge', 'double', 'premiere'),
    (3, 6650, 'rouge', 'double', 'premiere'),
    (4, 6650, 'rouge', 'double', 'bar'),
    (5, 6650, 'rouge', 'double', 'seconde'),
    (6, 6650, 'rouge', 'double', 'seconde'),
    (7, 6650, 'rouge', 'double', 'seconde'),
    (8, 6650, 'rouge', 'double', 'seconde'),
   
	(1, 6632, 'blanche', 'simple', 'premiere'),
    (2, 6632, 'blanche', 'simple', 'premiere'),
    (3, 6632, 'blanche', 'simple', 'premiere'),
    (4, 6632, 'blanche', 'simple', 'bar'),
    (5, 6632, 'blanche', 'simple', 'seconde'),
    (6, 6632, 'blanche', 'simple', 'seconde'),
    (7, 6632, 'blanche', 'simple', 'seconde'),
    (8, 6632, 'blanche', 'simple', 'seconde'),
	(11, 6632, 'blanche', 'simple', 'premiere'),
    (12, 6632, 'blanche', 'simple', 'premiere'),
    (13, 6632, 'blanche', 'simple', 'premiere'),
    (14, 6632, 'blanche', 'simple', 'bar'),
    (15, 6632, 'blanche', 'simple', 'seconde'),
    (16, 6632, 'blanche', 'simple', 'seconde'),
    (17, 6632, 'blanche', 'simple', 'seconde'),
    (18, 6632, 'blanche', 'simple', 'seconde'),
    
	(1, 6632, 'rouge', 'double', 'premiere'),
    (2, 6632, 'rouge', 'double', 'premiere'),
    (3, 6632, 'rouge', 'double', 'premiere'),
    (4, 6632, 'rouge', 'double', 'bar'),
    (5, 6632, 'rouge', 'double', 'seconde'),
    (6, 6632, 'rouge', 'double', 'seconde'),
    (7, 6632, 'rouge', 'double', 'seconde'),
    (8, 6632, 'rouge', 'double', 'seconde'),
	(11, 6632, 'rouge', 'double', 'premiere'),
    (12, 6632, 'rouge', 'double', 'premiere'),
    (13, 6632, 'rouge', 'double', 'premiere'),				
    (15, 6632, 'rouge', 'double', 'seconde'),
    (16, 6632, 'rouge', 'double', 'seconde'),
    (17, 6632, 'rouge', 'double', 'seconde'),
    (18, 6632, 'rouge', 'double', 'seconde');
        
INSERT INTO Reservation VALUES
	('ABCDEF', 'jane.smith@gmail.com', '2017-10-29 10:00:00', 324.615, 'Lyon', 'Avignon');
    
INSERT INTO PlaceReservee VALUES
	(21, 'ABCDEF', 2, 6607, 'rouge'),
    (22, 'ABCDEF', 2, 6607, 'rouge'),
    (23, 'ABCDEF', 2, 6607, 'rouge');