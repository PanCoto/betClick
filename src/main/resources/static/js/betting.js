document.addEventListener("DOMContentLoaded", () => {
    let slip = JSON.parse(localStorage.getItem("betclick_slip") || "[]");

    const slipContainer = document.getElementById("betslip-items-container");
    const emptyState = document.getElementById("betslip-empty-state");
    const calcPanel = document.getElementById("betslip-calculation");
    const totalOddsEl = document.getElementById("betslip-total-odds");
    const stakeInput = document.getElementById("betslip-stake");
    const potentialWinEl = document.getElementById("betslip-potential-win");
    const placeBetBtn = document.getElementById("place-bet-btn");
    const clearSlipBtn = document.getElementById("clear-betslip-btn");

    setupShowMoreLists();
    setupEventSearch();
    removeClosedSelectionsFromSlip();
    updateOddsButtonsState();
    renderSlip();

    function setupShowMoreLists() {
        document.querySelectorAll("[data-show-more]").forEach(container => {
            const limit = Math.max(1, parseInt(container.dataset.showMoreLimit || "8", 10));
            const step = Math.max(1, parseInt(container.dataset.showMoreStep || String(limit), 10));
            const itemSelector = container.dataset.showMoreItem || ":scope > *";
            const label = container.dataset.showMoreLabel || "Pokaż więcej";
            const items = Array.from(container.querySelectorAll(itemSelector));

            if (items.length <= limit) return;

            items.slice(limit).forEach(item => item.classList.add("expand-hidden"));

            const wrapper = document.createElement("div");
            wrapper.className = "show-more-wrap";

            const button = document.createElement("button");
            button.type = "button";
            button.className = "btn btn-show-more";

            const renderButtonLabel = () => {
                const hiddenCount = items.filter(item => item.classList.contains("expand-hidden")).length;
                button.innerHTML = `<i class="bi bi-chevron-down me-2"></i>${label} <span class="show-more-count">(${hiddenCount})</span>`;
            };

            button.addEventListener("click", () => {
                const hiddenItems = items.filter(item => item.classList.contains("expand-hidden"));
                hiddenItems.slice(0, step).forEach(item => item.classList.remove("expand-hidden"));

                if (!items.some(item => item.classList.contains("expand-hidden"))) {
                    wrapper.remove();
                    return;
                }

                renderButtonLabel();
            });

            renderButtonLabel();
            wrapper.appendChild(button);
            container.insertAdjacentElement("afterend", wrapper);
        });
    }

    function setupEventSearch() {
        const input = document.getElementById("event-search-input");
        const list = document.querySelector("[data-event-list]");
        if (!input || !list) return;

        const cards = Array.from(list.querySelectorAll(".event-card"));
        const searchableText = card => [
            card.dataset.eventName,
            card.dataset.teamA,
            card.dataset.teamB,
            card.dataset.leagueName,
            card.dataset.sportName
        ].filter(Boolean).join(" ").toLowerCase();

        input.addEventListener("input", () => {
            const query = input.value.trim().toLowerCase();
            cards.forEach(card => {
                const matches = !query || searchableText(card).includes(query);
                card.style.display = matches ? "" : "none";
            });
        });
    }

    function removeClosedSelectionsFromSlip() {
        const closedSelectionIds = new Set();
        const now = Date.now();

        document.querySelectorAll(".event-card").forEach(card => {
            const status = (card.dataset.eventStatus || "").toUpperCase();
            const eventEnd = Date.parse(card.dataset.eventEnd || "");
            const isAfterEnd = !Number.isNaN(eventEnd) && now >= eventEnd;
            const isClosed = status !== "UPCOMING" || isAfterEnd;

            if (!isClosed) return;

            if (status === "FINISHED" || isAfterEnd) {
                card.querySelectorAll("[data-finished-status]").forEach(badge => {
                    badge.classList.remove("d-none");
                });
            }

            card.querySelectorAll(".selection-odds-btn").forEach(btn => {
                const selectionId = parseInt(btn.dataset.selectionId, 10);
                if (!Number.isNaN(selectionId)) {
                    closedSelectionIds.add(selectionId);
                }
                btn.disabled = true;
                btn.dataset.eventClosed = "true";
                btn.classList.remove("active");
            });
        });

        if (closedSelectionIds.size === 0) return;

        const nextSlip = slip.filter(item => !closedSelectionIds.has(item.id));
        if (nextSlip.length !== slip.length) {
            slip = nextSlip;
            localStorage.setItem("betclick_slip", JSON.stringify(slip));
        }
    }

    document.querySelectorAll(".selection-odds-btn").forEach(btn => {
        btn.addEventListener("click", (e) => {
            const btnEl = e.currentTarget;
            if (btnEl.disabled || btnEl.dataset.eventClosed === "true") return;

            const selId = parseInt(btnEl.dataset.selectionId);
            const selName = btnEl.dataset.selectionName;
            const marketName = btnEl.dataset.marketName;
            const eventName = btnEl.dataset.eventName;
            const odds = parseFloat(btnEl.dataset.odds);

            const index = slip.findIndex(item => item.id === selId);

            if (index > -1) {
                slip.splice(index, 1);
            } else {
                const eventIndex = slip.findIndex(item => item.eventName === eventName);
                if (eventIndex > -1) {
                    slip.splice(eventIndex, 1);
                }
                
                slip.push({
                    id: selId,
                    name: selName,
                    marketName: marketName,
                    eventName: eventName,
                    odds: odds
                });
            }

            localStorage.setItem("betclick_slip", JSON.stringify(slip));
            updateOddsButtonsState();
            renderSlip();
        });
    });

    function updateOddsButtonsState() {
        document.querySelectorAll(".selection-odds-btn").forEach(btn => {
            if (btn.disabled || btn.dataset.eventClosed === "true") {
                btn.classList.remove("active");
                return;
            }

            const selId = parseInt(btn.dataset.selectionId);
            const isInSlip = slip.some(item => item.id === selId);
            if (isInSlip) {
                btn.classList.add("active");
            } else {
                btn.classList.remove("active");
            }
        });
    }

    function renderSlip() {
        if (!slipContainer) return;

        const slipItems = slipContainer.querySelectorAll(".betslip-item");
        slipItems.forEach(el => el.remove());

        if (slip.length === 0) {
            emptyState.style.display = "block";
            calcPanel.style.display = "none";
            return;
        }

        emptyState.style.display = "none";
        calcPanel.style.display = "block";

        let totalOdds = 1.00;

        slip.forEach(item => {
            totalOdds *= item.odds;

            const itemEl = document.createElement("div");
            itemEl.className = "betslip-item fade-in-el";
            itemEl.innerHTML = `
                <button class="betslip-remove" data-id="${item.id}">&times;</button>
                <div class="small text-secondary fw-semibold">${item.eventName}</div>
                <div class="small text-secondary">${item.marketName}</div>
                <div class="d-flex justify-content-between align-items-center mt-1">
                    <span class="badge bg-cyan bg-opacity-20 text-cyan">${item.name}</span>
                    <span class="fw-bold text-cyan">${item.odds.toFixed(2)}</span>
                </div>
            `;

            itemEl.querySelector(".betslip-remove").addEventListener("click", (e) => {
                const idToRemove = parseInt(e.currentTarget.dataset.id);
                slip = slip.filter(item => item.id !== idToRemove);
                localStorage.setItem("betclick_slip", JSON.stringify(slip));
                updateOddsButtonsState();
                renderSlip();
            });

            slipContainer.appendChild(itemEl);
        });

        totalOddsEl.textContent = totalOdds.toFixed(2);
        calculatePotentialWin(totalOdds);
    }

    function calculatePotentialWin(totalOdds) {
        if (!stakeInput || !potentialWinEl) return;
        const stake = parseFloat(stakeInput.value) || 0.00;
        const win = stake * totalOdds;
        potentialWinEl.textContent = win.toFixed(2) + " PLN";
    }

    if (stakeInput) {
        stakeInput.addEventListener("input", () => {
            let totalOdds = 1.00;
            slip.forEach(item => totalOdds *= item.odds);
            calculatePotentialWin(totalOdds);
        });
    }

    if (clearSlipBtn) {
        clearSlipBtn.addEventListener("click", () => {
            slip = [];
            localStorage.setItem("betclick_slip", JSON.stringify(slip));
            updateOddsButtonsState();
            renderSlip();
        });
    }

    if (placeBetBtn) {
        placeBetBtn.addEventListener("click", async () => {
            if (slip.length === 0) return;

            const selectionIds = slip.map(item => item.id);
            const stake = parseFloat(stakeInput.value);

            if (isNaN(stake) || stake < 0.10) {
                showToast("Minimalna stawka zakładu to 0.10 PLN!", "bg-danger");
                return;
            }

            placeBetBtn.disabled = true;
            placeBetBtn.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Wysyłanie...`;

            try {
                const response = await fetch("/api/bets", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify({
                        selectionIds: selectionIds,
                        stake: stake
                    })
                });

                if (response.ok) {
                    const result = await response.json();
                    showToast(`Kupon wniesiony pomyślnie! Numer kuponu: ${result.ticketNumber}`, "bg-success");
                    
                    slip = [];
                    localStorage.setItem("betclick_slip", JSON.stringify(slip));
                    updateOddsButtonsState();
                    renderSlip();

                    setTimeout(() => {
                        window.location.href = "/dashboard";
                    }, 2000);
                } else {
                    const error = await response.json();
                    showToast(`Błąd: ${error.message || "Nie można zawrzeć zakładu"}`, "bg-danger");
                }
            } catch (err) {
                showToast("Błąd połączenia z serwerem!", "bg-danger");
            } finally {
                placeBetBtn.disabled = false;
                placeBetBtn.textContent = "Wnieś zakład";
            }
        });
    }

    function showToast(message, bgClass) {
        const toastEl = document.getElementById("betclick-toast");
        const bodyEl = document.getElementById("toast-body-content");
        if (!toastEl || !bodyEl) return;

        bodyEl.textContent = message;
        toastEl.className = `toast align-items-center text-white border-0 ${bgClass}`;

        const toast = new bootstrap.Toast(toastEl);
        toast.show();
    }
});
