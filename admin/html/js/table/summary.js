import { tableApi } from "http://common.dev.ulake.usth.edu.vn/js/api.js";

// get group names. should be multiple columns but only one group for now.
function getCombineGroupCol(row, groupColIndices) {
    return groupColIndices
                .map(colIdx => row[colIdx])
                .join("-");
}

// TODO : refactor this function
function getGroupColIndices(cols) {
    let ret = [];
    for (let i = 0; i < cols.length; i++) {
        if (!!cols[i].groupBy) {
            ret.push(i);
        }
    }
    return ret;
}

// group rows into groups, as indicated by groupColIndices
function groupRows(rows, groupColIndices) {
    let ret = {};     // will be in structure { groupedItem: [rows] }
    for (const rid in rows) {
        const key = getCombineGroupCol(rows[rid], groupColIndices);
        if (!ret.hasOwnProperty(key)) {
            ret[key] = [];
        }
        ret[key].push(rows[rid]);
    }
    return ret;
}

// for each group, we perform average, min, and max
function calcStats(groups) {
    let ret = {};
    for (const groupName in groups) {
        let statsKey = {min: [], max: [], avg: [], sum: []};
        for (const row of groups[groupName]) {
            for (let i = 0; i < row.length; i++) {
                if (isNumeric(row[i])) {
                    const float = parseFloat(row[i]);
                    if (!statsKey.sum[i]) statsKey.sum[i] = 0;
                    if (!statsKey.min[i]) statsKey.min[i] = float;
                    if (!statsKey.max[i]) statsKey.max[i] = float;

                    if (statsKey.min[i] < float) statsKey.min[i] = float;
                    if (statsKey.max[i] > float) statsKey.max[i] = float;
                    statsKey.sum[i] += float;
                }
            }
        }
        statsKey.min = statsKey.min.map(i => i ? +i.toFixed(2) : "");
        statsKey.max = statsKey.max.map(i => i ? +i.toFixed(2) : "");
        statsKey.avg = statsKey.sum.map(i => i ? +(i/groups[groupName].length).toFixed(2) : "");
        delete statsKey.sum;
        ret[groupName] = statsKey;
    }
    return ret;
}

// generate summary from stats
function summarize(stats, groupColIndices) {
    for (const groupName in stats) {
        // a row for one group
        const groupStats = stats[groupName];
        for (const statKey in groupStats)    // min, max, avg
            groupStats[statKey][groupColIndices[0]] = `${groupName}: ${statKey}`;
    }
    return stats;
}

// perform summary for a location
function genSummaryRows(rows, cols) {
    let tableRows = [];
    const groupColIndices = getGroupColIndices(cols);
    if (groupColIndices.length) {
        const groups = groupRows(rows, groupColIndices);
        const stats = calcStats(groups);
        const summary = summarize(stats, groupColIndices);
        // convert this into table row
        for (const groupName in summary) {
            const groupStats = stats[groupName];
            for (const row in groupStats)
                tableRows.push(groupStats[row]);
        }
    }
    else {
        // show all by default
        tableRows.push(resp.rows[rid]);
    }
    // console.log(tableRows);
    return tableRows;
}

function drawTable(resp) {
    const header = $("thead tr")
    $("#name-detail").html(`Summary for environmental table "${resp.name}"`);

    // make columns
    resp.columns.forEach(col => {
        const th = $("<th></th>");
        th.html(col.columnName);
        header.append(th);
    });

    const tableRows = genSummaryRows(resp.rows, resp.columns);

    // post process each row
    $.fn.dataTable.ext.errMode = 'none';
    const table = $('#table').DataTable({
        scrollX: true,
        bProcessing: true,
        searching: false,
        paging: false,
        ordering: false,
        buttons: [ 'csv' ],
        data: tableRows
    });
    window.crud = { table: table };
}

async function ready() {
    const params = parseParam("id", "/tables");
    const id = parseInt(params.id);
    const data = await tableApi.data(id);
    drawTable(data);
}

$(document).ready(() => ready());