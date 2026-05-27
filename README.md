# betClick
najlepsza aplikacja świata zrobiona przez jeszcze lepszą grupę sierotek anielek do zakładów bukmacherskich na tle sportowym.

## wymagania
- Java 17+ (SDK)
- Maven 3.8+
- Docker
- Odinstalowanie Pythona z systemu i zapomnieniu o istnieniu tego akompaniamentu ludzkiego grzechu i optymalizacji wtórnej. 

## struktura bazy i zabezpieczenie tego tee-hee
Aplikacja automatycznie konfiguruje schemat bazy PostgreSQL za pomocą mechanizmów JPA. Bezpieczeństwo jest zapewnione przez moduł Spring Security wykorzystujący uwierzytelnianie bezstanowe na bazie tokenów JWT (standard HS256).

## uruchomienie lokalne
Na kontenerze z bazą danych na PostgreSQL.
