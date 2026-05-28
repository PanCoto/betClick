
.PHONY: help build up up-tools down restart logs \
        db-shell app-shell clean nuke ps health migrate


help:
	@echo ""
	@echo "  betClick — polecenia Docker"
	@echo "  ─────────────────────────────────────────────"
	@echo "  make build      Zbuduj obraz aplikacji"
	@echo "  make up         Uruchom postgres + app (tlo)"
	@echo "  make up-tools   Uruchom + pgAdmin (profil tools)"
	@echo "  make down       Zatrzymaj kontenery"
	@echo "  make restart    Restart tylko kontenera app"
	@echo "  make logs       Logi aplikacji na zywo"
	@echo "  make db-shell   psql do bazy (betclick_admin)"
	@echo "  make app-shell  Shell w kontenerze aplikacji"
	@echo "  make ps         Status kontenerow"
	@echo "  make health     Sprawdz health-check aplikacji"
	@echo "  make clean      Usun kontenery (zachowaj wolumeny)"
	@echo "  make nuke       Usun kontenery + wolumeny (DANE!)"
	@echo ""


build:
	docker compose build --no-cache app


up:
	@test -f .env || (echo "  Brak pliku .env! Skopiuj: cp .env.docker .env" && exit 1)
	docker compose up -d --build
	@echo "    Aplikacja startuje na http://localhost:8080"
	@echo "    Logi: make logs"


up-tools:
	@test -f .env || (echo "  Brak pliku .env! Skopiuj: cp .env.docker .env" && exit 1)
	docker compose --profile tools up -d --build
	@echo "    pgAdmin dostepny na http://localhost:5050"


down:
	docker compose --profile tools down

restart:
	docker compose restart app


logs:
	docker compose logs -f --tail=100 app


logs-all:
	docker compose logs -f --tail=50


db-shell:
	docker compose exec postgres psql -U betclick_admin -d betclick_db


app-shell:
	docker compose exec app sh


ps:
	docker compose ps


health:
	@curl -s -k https://localhost:8443/actuator/health | python3 -m json.tool \
	  || echo "   Aplikacja niedostepna lub curl/python3 nie zainstalowane"


clean:
	docker compose --profile tools down --remove-orphans
	@echo "   Wolumeny zachowane. Uzyj 'make nuke' aby usunac dane."


nuke:
	@echo "   UWAGA: Zostana usuniete wszystkie dane PostgreSQL!"
	@read -p "Czy na pewno? [tak/N]: " confirm; \
	  [ "$$confirm" = "tak" ] || (echo "Anulowano." && exit 1)
	docker compose --profile tools down -v --remove-orphans --rmi local
	@echo "   Wszystkie zasoby Docker usuniete."
