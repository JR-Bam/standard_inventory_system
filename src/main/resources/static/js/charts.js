/* =========================================================
   Dashboard charts — Chart.js setup
   Reads window.dashboardData, populated by dashboard.html
   ========================================================= */

(function () {
    'use strict';

    // ----- Theme tokens ------------------------------------------------
    const theme = {
        blue:    '#315B8C',
        coral:   '#F2765E',
        subtle:  '#413333',
        cream:   '#F5EFE3',
        danger:  '#a94442',
        tooltip: '#1d3757',
        grid:    'rgba(65, 51, 51, 0.08)',
        axis:    'rgba(65, 51, 51, 0.65)'
    };

    // Extended palette for doughnut slices
    const slicePalette = [
        theme.blue,
        theme.coral,
        theme.subtle,
        '#4E7CB0',
        '#E89B87',
        '#5C5454',
        '#7AA3D0',
        '#F0A995',
        '#8A8080',
        '#2E7D5B'
    ];

    // ----- Chart.js global defaults ------------------------------------
    Chart.defaults.font.family = "'Roboto', system-ui, sans-serif";
    Chart.defaults.font.size = 12;
    Chart.defaults.color = theme.axis;
    Chart.defaults.borderColor = theme.grid;
    Chart.defaults.plugins.legend.labels.usePointStyle = true;
    Chart.defaults.plugins.legend.labels.boxWidth = 8;
    Chart.defaults.plugins.legend.labels.padding = 16;
    Chart.defaults.plugins.legend.labels.color = theme.axis;
    Chart.defaults.plugins.tooltip.backgroundColor = theme.tooltip;
    Chart.defaults.plugins.tooltip.titleColor = theme.cream;
    Chart.defaults.plugins.tooltip.bodyColor = theme.cream;
    Chart.defaults.plugins.tooltip.padding = 10;
    Chart.defaults.plugins.tooltip.cornerRadius = 8;
    Chart.defaults.plugins.tooltip.titleFont = { weight: '600' };
    Chart.defaults.maintainAspectRatio = false;

    // ----- Data accessor -----------------------------------------------
    const data = window.dashboardData || {};
    const hasCategory = data.category && data.category.labels && data.category.labels.length;
    const hasMovements = data.movements && data.movements.labels && data.movements.labels.length;

    // =========================================================
    // Category doughnut
    // =========================================================
    let categoryChart = null;
    let currentMode = 'count'; // 'count' | 'value'

    function buildCategoryChart(mode) {
        const canvas = document.getElementById('categoryChart');
        if (!canvas) return;

        const labels = data.category.labels;
        const values = mode === 'count' ? data.category.counts : data.category.values;

        if (categoryChart) {
            categoryChart.destroy();
        }

        categoryChart = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: labels.map((_, i) => slicePalette[i % slicePalette.length]),
                    borderColor: '#fff',
                    borderWidth: 2,
                    hoverOffset: 6
                }]
            },
            options: {
                cutout: '62%',
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            boxWidth: 8,
                            padding: 14,
                            color: theme.axis
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                const val = ctx.parsed;
                                if (mode === 'value') {
                                    return ' ' + ctx.label + ': ₱' +
                                        Number(val).toLocaleString('en-PH', {
                                            minimumFractionDigits: 2,
                                            maximumFractionDigits: 2
                                        });
                                }
                                return ' ' + ctx.label + ': ' + val + ' item' +
                                    (val === 1 ? '' : 's');
                            }
                        }
                    }
                }
            }
        });
    }

    if (hasCategory) {
        buildCategoryChart(currentMode);

        const toggle = document.getElementById('categoryToggle');
        if (toggle) {
            toggle.addEventListener('click', function (e) {
                const btn = e.target.closest('.chart-toggle-btn');
                if (!btn) return;
                const mode = btn.dataset.mode;
                if (!mode || mode === currentMode) return;
                currentMode = mode;

                toggle.querySelectorAll('.chart-toggle-btn').forEach(function (b) {
                    b.classList.toggle('is-active', b === btn);
                });
                buildCategoryChart(mode);
            });
        }
    }

    // =========================================================
    // Movement line chart
    // =========================================================
    const movementCanvas = document.getElementById('movementChart');
    if (hasMovements && movementCanvas) {
        const labels = data.movements.labels.map(function (iso) {
            // ISO yyyy-MM-dd -> "Sep 21"
            const d = new Date(iso + 'T00:00:00');
            return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
        });

        function makeFillColor(hex) {
            const ctx = movementCanvas.getContext('2d');
            const gradient = ctx.createLinearGradient(0, 0, 0, 260);
            gradient.addColorStop(0, hexToRgba(hex, 0.28));
            gradient.addColorStop(1, hexToRgba(hex, 0.02));
            return gradient;
        }

        new Chart(movementCanvas, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Stock In',
                        data: data.movements.stockIn,
                        borderColor: theme.blue,
                        backgroundColor: makeFillColor(theme.blue),
                        borderWidth: 2.5,
                        tension: 0.35,
                        fill: true,
                        pointRadius: 0,
                        pointHoverRadius: 5,
                        pointHoverBackgroundColor: theme.blue,
                        pointHoverBorderColor: '#fff',
                        pointHoverBorderWidth: 2
                    },
                    {
                        label: 'Stock Out',
                        data: data.movements.stockOut,
                        borderColor: theme.coral,
                        backgroundColor: makeFillColor(theme.coral),
                        borderWidth: 2.5,
                        tension: 0.35,
                        fill: true,
                        pointRadius: 0,
                        pointHoverRadius: 5,
                        pointHoverBackgroundColor: theme.coral,
                        pointHoverBorderColor: '#fff',
                        pointHoverBorderWidth: 2
                    }
                ]
            },
            options: {
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                plugins: {
                    legend: {
                        position: 'top',
                        align: 'end',
                        labels: {
                            boxWidth: 8,
                            padding: 12,
                            color: theme.axis
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return ' ' + ctx.dataset.label + ': ' + ctx.parsed.y;
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: {
                            color: theme.axis,
                            maxRotation: 0,
                            autoSkipPadding: 20
                        }
                    },
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: theme.grid,
                            drawBorder: false
                        },
                        ticks: {
                            color: theme.axis,
                            precision: 0
                        }
                    }
                }
            }
        });
    }

    // ----- Helpers ------------------------------------------------------
    function hexToRgba(hex, alpha) {
        const h = hex.replace('#', '');
        const r = parseInt(h.substring(0, 2), 16);
        const g = parseInt(h.substring(2, 4), 16);
        const b = parseInt(h.substring(4, 6), 16);
        return 'rgba(' + r + ',' + g + ',' + b + ',' + alpha + ')';
    }
})();