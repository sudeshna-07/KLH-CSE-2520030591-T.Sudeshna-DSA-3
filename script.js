const dimensionsInput = document.getElementById("dimensions");
const optimizeBtn = document.getElementById("optimizeBtn");
const clearBtn = document.getElementById("clearBtn");
const datasetSelect = document.getElementById("datasetSelect");
const loadBtn = document.getElementById("loadBtn");
const downloadBtn = document.getElementById("downloadBtn");
const uploadInput = document.getElementById("uploadInput");
const message = document.getElementById("message");
const results = document.getElementById("results");
const prevStepBtn = document.getElementById("prevStepBtn");
const nextStepBtn = document.getElementById("nextStepBtn");

let datasetText = "";
let dpSteps = [];
let currentStep = 0;

function showMessage(text, isError = false) {
    message.textContent = text;
    message.className = "message " + (isError ? "error" : "success");
}

function formatNumber(value) {
    return Number(value).toLocaleString("en-IN");
}

async function readResponse(response) {
    const contentType = response.headers.get("content-type") || "";

    if (!contentType.includes("application/json")) {
        const body = await response.text();
        throw new Error(
            "Expected JSON but received another response. " +
            "Check that WebServer.java is running and open the website at http://localhost:8080. " +
            body.slice(0, 80)
        );
    }

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || "The request failed.");
    }

    return data;
}

/* Read CSV data, including fields enclosed in quotation marks. */
function parseCSV(text) {
    const rows = [];
    let row = [];
    let field = "";
    let quoted = false;

    for (let i = 0; i < text.length; i++) {
        const ch = text[i];

        if (ch === '"') {
            if (quoted && text[i + 1] === '"') {
                field += '"';
                i++;
            } else {
                quoted = !quoted;
            }
        } else if (ch === "," && !quoted) {
            row.push(field);
            field = "";
        } else if ((ch === "\n" || ch === "\r") && !quoted) {
            if (ch === "\r" && text[i + 1] === "\n") {
                i++;
            }

            row.push(field);

            if (row.some(cell => cell.trim() !== "")) {
                rows.push(row);
            }

            row = [];
            field = "";
        } else {
            field += ch;
        }
    }

    row.push(field);

    if (row.some(cell => cell.trim() !== "")) {
        rows.push(row);
    }

    return rows;
}

/* Load the dataset from the Java backend. */
async function loadDataset() {
    try {
        const response = await fetch("/api/dataset");
        const data = await readResponse(response);

        datasetText = data.csv;
        const rows = parseCSV(datasetText);

        datasetSelect.innerHTML = "";

        rows.slice(1).forEach(row => {
            if (row.length < 2 || !row[0].trim()) {
                return;
            }

            const option = document.createElement("option");
            option.value = row[1].trim();
            option.textContent =
                row[0] + " — " + row[1].trim() +
                (row[2] ? " (" + row[2] + ")" : "");

            datasetSelect.appendChild(option);
        });

        if (!datasetSelect.options.length) {
            datasetSelect.innerHTML =
                '<option value="">No test cases found</option>';
        }

        showMessage("Dataset loaded successfully.");
    } catch (error) {
        showMessage("Could not load dataset: " + error.message, true);
    }
}

/* Load a selected test case into the dimensions field. */
loadBtn.addEventListener("click", () => {
    if (!datasetSelect.value) {
        showMessage("Please select a test case.", true);
        return;
    }

    dimensionsInput.value = datasetSelect.value;
    showMessage("Test case loaded. Click Run Optimization.");
});

/* Run the matrix chain optimization. */
optimizeBtn.addEventListener("click", async () => {
    const dimensions = dimensionsInput.value.trim();

    if (!dimensions) {
        showMessage("Please enter the matrix dimensions.", true);
        return;
    }

    optimizeBtn.disabled = true;
    optimizeBtn.textContent = "Calculating...";

    try {
        const response = await fetch("/api/optimize", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ dimensions })
        });

        const data = await readResponse(response);

        document.getElementById("optimalCost").textContent =
            formatNumber(data.optimalCost);

        document.getElementById("leftCost").textContent =
            formatNumber(data.leftToRightCost);

        document.getElementById("savedCost").textContent =
            formatNumber(data.costSaved);

        document.getElementById("savedPercent").textContent =
            Number(data.percentSaved).toFixed(2) + "% reduction";

        document.getElementById("matrixCount").textContent =
            data.matrixCount;

        document.getElementById("parenthesization").textContent =
            data.parenthesization;

        renderTable("costTable", data.costTable, "Minimum Cost");
        renderTable("splitTable", data.splitTable, "Split Position");
        renderCostChart(data);
        buildDPSteps(dimensions, data);

        results.hidden = false;

        showMessage("Optimization completed successfully.");

        results.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    } catch (error) {
        showMessage(error.message, true);
    } finally {
        optimizeBtn.disabled = false;
        optimizeBtn.textContent = "Run Optimization";
    }
});

/* Create the cost comparison chart. */
function renderCostChart(data) {
    const chart = document.getElementById("costChart");
    const summary = document.getElementById("chartSummary");

    chart.replaceChildren();

    const values = [
        {
            label: "Left-to-right",
            cost: Number(data.leftToRightCost),
            className: "bar-left"
        },
        {
            label: "Optimal DP",
            cost: Number(data.optimalCost),
            className: "bar-optimal"
        }
    ];

    const maximum = Math.max(
        ...values.map(item => item.cost),
        1
    );

    values.forEach(item => {
        const group = document.createElement("div");
        group.className = "chart-bar-group";

        const heading = document.createElement("div");
        heading.className = "chart-bar-heading";

        const label = document.createElement("span");
        label.textContent = item.label;

        const value = document.createElement("strong");
        value.textContent = formatNumber(item.cost);

        heading.append(label, value);

        const track = document.createElement("div");
        track.className = "chart-bar-track";

        const bar = document.createElement("div");
        bar.className = "chart-bar " + item.className;
        bar.style.width = (item.cost / maximum * 100) + "%";

        track.appendChild(bar);
        group.append(heading, track);
        chart.appendChild(group);
    });

    summary.textContent =
        "The optimal strategy saves " +
        formatNumber(data.costSaved) +
        " scalar multiplications (" +
        Number(data.percentSaved).toFixed(2) +
        "% compared with left-to-right multiplication).";
}

/* Build a beginner-friendly DP walkthrough. */
function buildDPSteps(dimensionsText, data) {
    const p = dimensionsText
        .trim()
        .split(/[\s,]+/)
        .map(Number);

    const n = p.length - 1;

    dpSteps = [];
    currentStep = 0;

    dpSteps.push({
        title: "Step 1: Understand the input",
        description:
            "The dimension sequence is " + p.join(", ") +
            ". It represents " + n +
            " matrices. Matrix A1 has dimensions " +
            p[0] + " × " + p[1] +
            ". Each following matrix uses two consecutive dimensions.",
        formula:
            "A1 = " + p[0] + " × " + p[1]
    });

    dpSteps.push({
        title: "Step 2: Initialize the DP table",
        description:
            "A single matrix does not need to be multiplied by another matrix. " +
            "Therefore, the cost of every single-matrix interval is zero.",
        formula:
            "m[i][i] = 0"
    });

    if (n >= 2) {
        dpSteps.push({
            title: "Step 3: Calculate chains of length 2",
            description:
                "For two adjacent matrices, there is only one multiplication order. " +
                "For example, multiplying A1 and A2 costs " +
                p[0] + " × " + p[1] + " × " + p[2] +
                " = " + formatNumber(p[0] * p[1] * p[2]) +
                " scalar multiplications.",
            formula:
                "m[1][2] = p[0] × p[1] × p[2] = " +
                formatNumber(p[0] * p[1] * p[2])
        });
    }

    for (let length = 3; length <= n; length++) {
        dpSteps.push({
            title: "Step " + (dpSteps.length + 1) +
                ": Evaluate chains of length " + length,
            description:
                "Consider every consecutive group of " + length +
                " matrices. Try all possible split positions. " +
                "For each split, add the left subchain cost, " +
                "the right subchain cost, and the cost of multiplying " +
                "the two resulting matrices. Keep the smallest total.",
            formula:
                "m[i][j] = min { m[i][k] + m[k+1][j] " +
                "+ p[i] × p[k+1] × p[j+1] }"
        });
    }

    dpSteps.push({
        title: "Final Step: Read the optimal answer",
        description:
            "After all subchains have been evaluated, the minimum cost " +
            "for the complete chain is stored in the top-right entry " +
            "of the cost table. The split table helps reconstruct the " +
            "best multiplication order.",
        formula:
            "Minimum cost = " + formatNumber(data.optimalCost) +
            "\nOptimal order = " + data.parenthesization
    });

    renderDPStep();
}

/* Display the currently selected walkthrough step. */
function renderDPStep() {
    const content = document.getElementById("dpStepContent");
    const counter = document.getElementById("stepCounter");
    const progress = document.getElementById("stepProgressFill");

    if (!dpSteps.length) {
        return;
    }

    const step = dpSteps[currentStep];

    content.replaceChildren();

    const title = document.createElement("h3");
    title.textContent = step.title;

    const description = document.createElement("p");
    description.textContent = step.description;

    const formula = document.createElement("div");
    formula.className = "dp-formula";
    formula.textContent = step.formula;

    content.append(title, description, formula);

    counter.textContent =
        "Step " + (currentStep + 1) + " of " + dpSteps.length;

    progress.style.width =
        ((currentStep + 1) / dpSteps.length * 100) + "%";

    prevStepBtn.disabled = currentStep === 0;
    nextStepBtn.disabled = currentStep === dpSteps.length - 1;
}

prevStepBtn.addEventListener("click", () => {
    if (currentStep > 0) {
        currentStep--;
        renderDPStep();
    }
});

nextStepBtn.addEventListener("click", () => {
    if (currentStep < dpSteps.length - 1) {
        currentStep++;
        renderDPStep();
    }
});

/* Display the cost and split tables returned by Java. */
function renderTable(tableId, values, firstHeader) {
    const table = document.getElementById(tableId);
    table.replaceChildren();

    if (!Array.isArray(values)) {
        return;
    }

    const thead = document.createElement("thead");
    const headerRow = document.createElement("tr");

    const first = document.createElement("th");
    first.textContent = firstHeader;
    headerRow.appendChild(first);

    for (let i = 0; i < values.length; i++) {
        const th = document.createElement("th");
        th.textContent = "A" + (i + 1);
        headerRow.appendChild(th);
    }

    thead.appendChild(headerRow);
    table.appendChild(thead);

    const tbody = document.createElement("tbody");

    values.forEach((row, i) => {
        const tr = document.createElement("tr");

        const rowHeader = document.createElement("th");
        rowHeader.textContent = "A" + (i + 1);
        tr.appendChild(rowHeader);

        row.forEach(value => {
            const td = document.createElement("td");

            td.textContent =
                value === null || value === -1
                    ? "—"
                    : formatNumber(value);

            tr.appendChild(td);
        });

        tbody.appendChild(tr);
    });

    table.appendChild(tbody);
}

/* Clear the current input and results. */
clearBtn.addEventListener("click", () => {
    dimensionsInput.value = "";
    results.hidden = true;

    dpSteps = [];
    currentStep = 0;

    prevStepBtn.disabled = true;
    nextStepBtn.disabled = true;

    document.getElementById("dpStepContent").innerHTML =
        "<h3>Run optimization first</h3>" +
        "<p>Enter dimensions and click Run Optimization to begin the walkthrough.</p>";

    document.getElementById("stepCounter").textContent =
        "Run optimization to begin";

    document.getElementById("stepProgressFill").style.width = "0%";

    showMessage("");
});

/* Download the current CSV dataset. */
downloadBtn.addEventListener("click", () => {
    if (!datasetText) {
        showMessage("The dataset has not loaded yet.", true);
        return;
    }

    const blob = new Blob(
        [datasetText],
        { type: "text/csv;charset=utf-8;" }
    );

    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");

    link.href = url;
    link.download = "matrix_dataset.csv";

    document.body.appendChild(link);
    link.click();
    link.remove();

    URL.revokeObjectURL(url);
});

/* Upload a CSV file and save it through the Java backend. */
uploadInput.addEventListener("change", async () => {
    const file = uploadInput.files[0];

    if (!file) {
        return;
    }

    try {
        const csv = await file.text();

        const response = await fetch("/api/dataset", {
            method: "POST",
            headers: {
                "Content-Type": "text/plain;charset=utf-8"
            },
            body: csv
        });

        await readResponse(response);
        await loadDataset();

        showMessage("Dataset uploaded and saved successfully.");
    } catch (error) {
        showMessage("CSV upload failed: " + error.message, true);
    } finally {
        uploadInput.value = "";
    }
});

/* Load the dataset when the page starts. */
loadDataset();