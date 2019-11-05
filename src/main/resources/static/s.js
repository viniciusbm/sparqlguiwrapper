"use strict";

(function() {

    var cm = null;
    var cmErrorMark = null;
    var dirty = false;
    var checkOnNextPing = false;
    var lastChange = new Date(0);
    var errorTimeout = null;

    var tickInterval = 200;
    var pingInterval = 500;
    var errorDisplayDelay = 1000;

    function init() {
        var field = document.getElementById('query-field');
        cm = CodeMirror.fromTextArea(field, {
            lineNumbers: true,
            mode: 'sparql',
            smartIndent: false, // does not work properly
            extraKeys: {
                Tab: function(cm) {
                    var spaces = Array(cm.getOption('indentUnit') + 1).join(' ');
                    cm.replaceSelection(spaces);
                }
            },
        });
        cm.on('change', function(c) {
            lastChange = new Date();
            dirty = true;
            clearInlineError();
        });

        document.getElementById('submit-btn').onclick = submitQuery;

        getOldQuery();
        setTimeout(tick, tickInterval);
        setTimeout(ping, pingInterval);
    }

    window.onload = init;

    function clearTable(t) {
        t.deleteTFoot();
        t.deleteTHead();
        for (var tb of document.getElementsByTagName('tbody')) {
            var e = document.createElement('tbody');
            tb.parentNode.replaceChild(e, tb);
        }
    }

    function showResults(j) {
        var t = document.getElementById('result-table');
        clearTable(t);
        if (j['error']) {
            var m = document.getElementById('error-result-msg');
            m.style.visibility = 'visible';
            m.innerText = j['error'];
            return;
        }
        t.createTHead();
        document.getElementById('running-and-count').innerText = '';
        var tr = t.tHead.insertRow(-1);
        tr.appendChild(document.createElement('th'));
        for (var c of j['columns']) {
            var th = document.createElement('th');
            th.innerText = c;
            tr.appendChild(th);
        }
        var n = 0;
        for (var row of j['results']) {
            var tr = t.tBodies[0].insertRow(-1);
            var td = tr.insertCell(-1);
            td.innerText = (++n) + '.';
            for (var c of row) {
                var td = tr.insertCell(-1);
                var s = document.createElement('span');
                td.appendChild(s);
                if (c['isLiteral']) {
                    td.className = 'literal';
                    var q = typeof c['value'] === 'string' ? '"' : '';
                    td.innerText = q + c['value'] + q;
                    s.className = 'type';
                    s.innerText = c['type'];
                    s.innerHTML = '<b>Type: </b>' + s.innerHTML;
                } else {
                    if (typeof c['fullName'] !== 'undefined') {
                        td.className = 'resource';
                        td.innerText = c['localName'];
                        s.className = 'fullName';
                        s.innerText = c['fullName'];
                        s.innerHTML = '<b>URI: </b><br/>' + s.innerHTML;
                    } else {
                        td.innerText = '';
                        td.className = 'nullcell';
                        s.innerHTML = '<b>NULL</b>';
                        s.className = 'nulltooltip';
                    }
                }
                td.appendChild(s);
            }
        }
        document.getElementById('running-and-count').innerText =
            (n == 0 ? 'No results.' :
             n == 1 ? 'One result.' : (n + ' results.'));
    }

    function submitQuery() {
        document.getElementById('submit-btn').style.
            visibility = "hidden";
        var query = cm.getValue();
        clearTable(document.getElementById('result-table'));
        document.getElementById('running-and-count').innerText = '';
            'Running&hellip;';
        var el = document.getElementById('error-result-msg');
        el.style.visibility = 'hidden';
        el.innerText = '';
        fetch('/run-query', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query: query
            })
        }).then(async function(r) {
            if (!r.ok)
                throw new Error((r.responseText || '') + "\n" +
                    await r.text());
            return r.json();
        }).then(function(j) {
            showResults(j);
            document.getElementById('submit-btn').style.
                visibility = "visible";
        }).catch(function(e) {
            document.getElementById('submit-btn').style.
                visibility = "visible";
            var m = document.getElementById('error-result-msg');
            m.style.visibility = 'visible';
            m.innerText = 'Unknown error.';
        });
    }

    function getOldQuery() {
        checkOnNextPing = false;
        fetch('/get-last-query').then(async function(r){
            if (!r.ok)
                throw new Error(r.responseText + "\n" + await r.text());
            return r.json();
        }).then(function(j) {
            if (j['query'])
                cm.setValue(j['query']);
        }).catch(function(e) {
            alert('Please reload the page.\n' + e);
        });
    }


    function ping() {
        var query = checkOnNextPing ? cm.getValue() : '';
        checkOnNextPing = false;
        fetch('/ping', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                query: query
            })
        }).then(async function(r) {
            if (!r.ok)
                throw new Error(r.responseText + "\n" + await r.text());
            var el = document.querySelector('#connection-msg .yes');
            el.style.display = 'inline';
            el = document.querySelector('#connection-msg .no');
            el.style.display = 'none';
            return r.json();
        }).then(function(j) {
            var fnLabel = document.getElementById('ontology-fn');
            var uriLabel = document.getElementById('ontology-uri');
            if (fnLabel.innerText != (j['fileName'] || '[NOT SET]'))
                fnLabel.innerText = j['fileName'] || '[NOT SET]';
            if (uriLabel.innerText != (j['URI'] || '[UNKNOWN]'))
                uriLabel.innerText = j['URI'] || '[UNKNOWN]';
            var err = j['error'];
            if (err && !checkOnNextPing) {
                clearTimeout(errorTimeout);
                var delay = lastChange - new Date() + errorDisplayDelay;
                errorTimeout = setTimeout(function() {
                    showInlineError(err);
                }, delay);
            } else {
                if (query) { // don't clear if nothing was checked
                    clearTimeout(errorTimeout);
                    clearInlineError();
                }
            }
            setTimeout(ping, pingInterval);
        }).catch(function(e) {
            setTimeout(ping, pingInterval);
            var el = document.querySelector('#connection-msg .no');
            el.style.display = 'inline';
            el = document.querySelector('#connection-msg .yes');
            el.style.display = 'none';
        });
    }

    function tick() {
        if (dirty) {
            dirty = false;
            checkOnNextPing = true;
        }
        setTimeout(tick, tickInterval);
    }


    function clearInlineError() {
        for (var m of cm.getAllMarks())
            m.clear();
        var icon = document.getElementById('error-icon');
        icon.style.visibility = 'hidden';
    }

    function showInlineError(err) {
        var p = /line (\d+), column (\d+)/i.exec(err);
        if (p) {
            var line, col, lineF, colF, ltext;
            if (err.indexOf('Unresolved prefixed name') == -1) {
                lineF = parseInt(p[1]) - 1;
                colF = parseInt(p[2]);
                line = lineF;
                ltext = cm.getLine(line).substr(0, colF - 1);
                col = ((/.*\s/.exec(ltext) || [[]])[0]).length;
            } else {
                line = parseInt(p[1]) - 1;
                col = parseInt(p[2]) - 1;
                lineF = line;
                ltext = cm.getLine(line).substr(col);
                colF = col +
                    ((/[^\s\{\}\(\)\?]+/.exec(ltext) || [[]])
                    [0]).length;
            }
            clearInlineError();
            cmErrorMark = cm.markText({
                'line': line,
                'ch': col
            }, {
                'line': lineF,
                'ch': colF
            }, {
                'className': 'error',
                'endStyle': 'error-icon',
                'clearOnEnter': false,
                'clearWhenEmpty': false
            });
            var icon = document.getElementById('error-icon');
            var charPos = cm.charCoords({
                'line': lineF,
                'ch': colF
            }, 'page');
            icon.style.visibility = 'visible';
            icon.style.top = charPos.bottom + 'px';
            icon.style.left = charPos.left + 'px';
            document.getElementById('inline-error-msg').innerText = err;
        }
    }


})();
