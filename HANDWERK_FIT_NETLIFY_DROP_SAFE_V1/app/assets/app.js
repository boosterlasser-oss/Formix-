// PATCH_TERMS_DEFAULT: Standard-Fachwörter für alle Übungen
const TERMS_DEFAULT = [
  {t:'Satz / Sätze', d:'Eine Gruppe von Wiederholungen am Stück. Beispiel: 3 Sätze à 10 Wdh.'},
  {t:'Wdh. (Wiederholungen)', d:'Wie oft du die Bewegung pro Satz ausführst (z. B. 8–12).'},
  {t:'Tempo', d:'Wie schnell du bewegst. Kontrolliert = sicherer und effektiver.'}
];
const $=s=>document.querySelector(s);
const today=new Date(); const iso=d=>d.toISOString().slice(0,10);
$("#today").textContent=today.toLocaleDateString("de-DE");
$("#logDate").value=iso(today);

const KEY_PROFILE="hwfit_profile_v4s";
const KEY_PLAN="hwfit_plan_v4s";
const KEY_SESSION="hwfit_session_v4s";
const KEY_LOG="hwfit_log_v4s";
const KEY_ONBOARD="hwfit_onboard_done_v1";
const load=(k,f)=>{try{return JSON.parse(localStorage.getItem(k)||"")??f}catch(e){return f}};
const save=(k,v)=>localStorage.setItem(k, JSON.stringify(v));

document.querySelectorAll(".tab[data-tab]").forEach(btn=>{
  btn.addEventListener("click", ()=>{
    document.querySelectorAll(".tab[data-tab]").forEach(b=>b.classList.remove("active"));
    btn.classList.add("active");
    const id=btn.dataset.tab;
    ["setup","plan","kalender"].forEach(t=>$("#tab-"+t).style.display=(t===id)?"":"none");
    if(id==="plan") renderPlan();
    if(id==="kalender") renderHistory();
  });
});

function esc(s){return String(s??"").replace(/&/g,"&amp;").replace(/</g,"&lt;")}

function renderProfileUI(){
  const {age,bmi}=calcMetrics(profile);
  const bmiTxt = bmi!=null ? (Math.round(bmi*10)/10).toString().replace('.',',') : "-";
  const ageTxt = age!=null ? age : "-";
  const goalMap = {lose:"Abnehmen",build:"Muskeln aufbauen",fit:"Fit bleiben"};
  const goalTxt = goalMap[profile.mainGoal] || "-";

  $("#profileSummary").innerHTML =
    `<b>Alter:</b> ${esc(ageTxt)} · <b>BMI:</b> ${esc(bmiTxt)} · <b>Ziel:</b> ${esc(goalTxt)}<br>`+
    `<span style="opacity:.85">Änderst du Gewicht/Größe/Körperform, kannst du den Plan direkt neu berechnen.</span>`;

  const year=(new Date()).getFullYear();
  const opts = (arr, val)=>arr.map(([v,t])=>`<option value="${esc(v)}" ${String(v)===String(val)?'selected':''}>${esc(t)}</option>`).join('');
  const pick = (min,max,val)=>{
    const a=[];
    for(let i=min;i<=max;i++) a.push(`<option value="${i}" ${String(i)===String(val)?'selected':''}>${i}</option>`);
    return a.join('');
  };

  $("#profileForm").innerHTML = `
    <label>Hauptziel</label>
    <select id="pf_mainGoal">
      ${opts([["lose","Abnehmen"],["build","Muskeln aufbauen"],["fit","Fit bleiben"]], profile.mainGoal)}
    </select>

    
    <label>Trainingslevel</label>
    <select id="pf_experience">
      ${opts([["new","Anfänger"],["some","Fortgeschrittene"],["pro","Profi"]], profile.experience)}
    </select>
<label>Geburtsjahr</label>
    <select id="pf_birthYear">${pick(1940, year, profile.birthYear||"")}</select>

    <label>Größe (cm)</label>
    <input id="pf_height" inputmode="numeric" type="number" min="130" max="220" step="1" value="${esc(profile.heightCm??"")}">

    <label>Gewicht (kg)</label>
    <input id="pf_weight" inputmode="decimal" type="number" min="35" max="250" step="0.1" value="${esc(profile.weightKg??"")}">

    <label>Zielgewicht (kg)</label>
    <input id="pf_targetWeight" inputmode="decimal" type="number" min="35" max="250" step="0.1" value="${esc(profile.targetWeightKg??"")}">

    <label>Körperform aktuell (1 = schlank, 5 = mollig)</label>
    <select id="pf_bodyNow">${pick(1,5, profile.bodyFormNow||3)}</select>

    <label>Körperform Ziel (1 = schlank, 5 = mollig)</label>
    <select id="pf_bodyGoal">${pick(1,5, profile.bodyFormGoal||3)}</select>
  `;
}

const questions=[
  // 01 ZIEL UND FOKUS
  {id:"gender", type:"choice", title:"Welches Geschlecht haben Sie?", hint:"Damit wir den Plan besser anpassen können.", opts:[
    ["male","Männlich"],["female","Weiblich"],["divers","Divers"]
  ]},
  {id:"mainGoal", type:"choice", title:"Was ist Ihr Hauptziel?", hint:"Wähle das Ziel, das dir aktuell am wichtigsten ist.", opts:[
    ["lose","Abnehmen"],["build","Bauen Sie Muskeln auf"],["fit","Fit bleiben"]
  ]},
  {id:"focusArea", type:"choice", title:"Um welchen Bereich geht es Ihnen?", hint:"Wähle den Schwerpunkt – der Plan passt sich an.", opts:[
    ["full","Ganzer Körper"],["arm","Arm"],["chest","Brust"],["abs","Bauchmuskeln"],["legs","Bein"]
  ]},
  {id:"motivation", type:"choice", title:"Was motiviert Sie am meisten?", hint:"Eine Auswahl reicht.", opts:[
    ["look","Noch attraktiver aussehen"],["strong","Stärker werden"],["health","Gesünder leben"],["confidence","Mehr Selbstbewusstsein"],["energy","Mehr Energie"],["stress","Stress abbauen"]
  ]},
  {id:"experience", type:"choice", title:"Welches Trainingslevel haben Sie?", hint:"Damit wir Sätze, Wdh. und Pausen passend anpassen.", opts:[
    ["new","Anfänger"],["some","Fortgeschrittene"],["pro","Profi"]
  ]},


  // 02 ÜBER DEINEN KÖRPER
  {id:"birthYear", type:"number", title:"In welchem Jahr sind Sie geboren?", hint:"Damit wir Ihr Training bestmöglich an Ihre Altersgruppe anpassen.", min:1940, max:(new Date()).getFullYear(), step:1, unit:""},
  {id:"heightCm", type:"number", title:"Wie groß sind Sie?", hint:"Angabe in Zentimetern.", min:130, max:220, step:1, unit:"cm"},
  {id:"weightKg", type:"number", title:"Welches Gewicht haben Sie im Augenblick?", hint:"Angabe in Kilogramm.", min:35, max:250, step:0.1, unit:"kg"},
  {id:"targetWeightKg", type:"number", title:"Was ist dein Ziel-Gewicht?", hint:"Realistisch wählen – du kannst später anpassen.", min:35, max:250, step:0.1, unit:"kg"},
  {id:"bodyFormNow", type:"range", title:"Welche Körperform passt derzeit zu Ihnen?", hint:"Schieberegler: Schlank ↔ Mollig.", min:1, max:5, step:1, leftLabel:"Schlank", rightLabel:"Mollig"},
  {id:"bodyFormGoal", type:"range", title:"Was ist deine Ziel-Körperform?", hint:"Schieberegler: Schlank ↔ Mollig.", min:1, max:5, step:1, leftLabel:"Schlank", rightLabel:"Mollig"},

  // 03 PHYSISCHE BEWERTUNG
  {id:"job", type:"choice", title:"Was beschreibt dich am besten?", hint:"Das hilft bei der Alltags-Belastung.", opts:[
    ["student","Student"],
    ["fulltime","Vollzeitangestellter"],
    ["parttime","Teilzeitarbeiter"],
    ["freelance","Freiberufler"],
    ["home","Hausfrau / Hausmann"],
    ["owner","Unternehmer"],
    ["shift","Arbeiter mit Schicht- oder unregelmäßigen Arbeitszeiten"],
    ["between","Zwischen zwei Jobs"],
    ["change","In einem Lebensumbruch"],
    ["other","Sonstiges"]
  ]},
  {id:"trainWhere", type:"choice", title:"Wo trainierst du normalerweise?", hint:"Damit wir den Plan an deinen Ort anpassen.", opts:[
    ["gym","Im Fitnessstudio (Geräte)"],
    ["mat","Auf der Yogamatte"],["bed","Auf dem Bett"],["any","Zuhause (freie Übungen)"]
  ]}
];
let idx=0;
let profile=load(KEY_PROFILE,{experience:"new",workload:"mid",timePref:"45",equipment:"minimal",focus:"balance",pain:"none"});

function num(v){
  const n=Number(String(v??"").replace(',','.'));
  return Number.isFinite(n) ? n : null;
}
function calcMetrics(p){
  const by = num(p.birthYear);
  const h  = num(p.heightCm);
  const w  = num(p.weightKg);
  const year=(new Date()).getFullYear();
  const age = by ? Math.max(10, Math.min(95, year - by)) : null;
  const bmi = (h && w) ? (w / Math.pow(h/100,2)) : null;
  return {age, bmi};
}// Gewicht-/BMI-/Alter-Score: sorgt dafür, dass Gewichtsänderungen sichtbar den Plan ändern.
// score > 0 = leichter / leistungsfähiger → mehr Wdh., kürzere Pausen
// score < 0 = schwerer / höhere Belastung → weniger Wdh., längere Pausen
function loadScore(p){
  const {age,bmi}=calcMetrics(p);
  // BMI-Teil (hauptsächlich abhängig von Gewicht)
  let s=0;
  if(bmi!=null){
    // Referenz-BMI 26 (leicht über Normal) als Mitte.
    // je 2 BMI-Punkte ≈ 1 Score-Schritt, clamp auf [-3..+3]
    s += (26 - bmi)/2;
  }
  // Alter: ab 45 schrittweise vorsichtiger
  if(age!=null){
    s += (45 - age)/10;
  }
  // Zielgewicht: wenn Ziel deutlich niedriger als aktuell → eher Fatloss-Ansatz (mehr Wiederholungen, aber ggf. weniger Sätze)
  const w=num(p.weightKg), tw=num(p.targetWeightKg);
  if(w!=null && tw!=null){
    const diff = (w - tw); // positiv = will abnehmen
    s += (-diff)/15; // 15kg ≈ 1 Schritt (abnehmen → etwas negativer, weil höhere Belastung am Anfang)
  }

  // Körperform (1=schlank … 5=mollig) als zusätzlicher, nutzergeführter Hinweis
  const bfNow = num(p.bodyFormNow);
  const bfGoal = num(p.bodyFormGoal);
  if(bfNow!=null){
    // Mittelpunkt 3. Schlank → positiver Score, Mollig → negativer Score
    s += (3 - bfNow) * 0.6;
  }
  if(bfNow!=null && bfGoal!=null){
    // Wenn Ziel deutlich schlanker als aktuell → am Anfang vorsichtiger
    const d = (bfNow - bfGoal); // >0: will schlanker werden
    s += (-d) * 0.5;
  }

  // Ziel-Fokus
  if(p.mainGoal==="lose") s -= 0.4;
  if(p.mainGoal==="build") s += 0.4;

  // Alltag/Ort (aus Onboarding)
  if(p.workload==="high") s -= 0.4;
  if(p.workload==="low") s += 0.2;
  if(p.trainWhere==="bed") s -= 0.3;
  // clamp & runden auf ganze Schritte
  s = Math.max(-3, Math.min(3, s));
  return Math.round(s);
}

function setsPlan(p){
  const lvl=planLevel(p);

  // Basis nach Level (Anfänger bewusst niedrig)
  let main = (lvl==="new"?2:(lvl==="some"?3:4));
  let extra = (lvl==="new"?1:2);
  let core = (lvl==="new"?1:2);

  const sc = loadScore(p);

  // schwere/ältere Profile: weniger Volumen
  if(sc<=-2){
    main=Math.max(1, main-1);
    extra=Math.max(0, extra-1);
    core=Math.max(0, core-1);
  }else if(sc===-1){
    extra=Math.max(0, extra-1);
  }

  // leichte/fitte Profile: etwas mehr Volumen – aber NICHT bei Anfänger aufblasen
  if(lvl!=="new" && sc>=2){
    main=Math.min(5, main+1);
    extra=Math.min(3, extra+1);
  }

  // Ziel: Muskelaufbau bekommt mehr Arbeit – aber Anfänger bleibt Anfänger
  if(lvl!=="new" && p.mainGoal==="build"){
    main=Math.min(5, main+1);
    extra=Math.min(3, extra+1);
  }

  // Workload/Ort
  if(p.workload==="high") extra=Math.max(0, extra-1);
  if(p.trainWhere==="bed") main=Math.max(1, main-1);

  // harte Caps
  if(lvl==="new"){
    main=Math.min(2, main);
    extra=Math.min(1, extra);
    core=Math.min(1, core);
  }

  return {main, extra, core};
}

function adjustRange(rangeStr, delta){
  const s=String(rangeStr||"");
  const m=s.match(/(\d+(?:[\.,]\d+)?)\s*[–-]\s*(\d+(?:[\.,]\d+)?)(.*)/);
  if(!m) return s;
  const a=parseFloat(m[1].replace(',','.'));
  const b=parseFloat(m[2].replace(',','.'));
  const tail=m[3]||"";
  const na=Math.max(1, a+delta);
  const nb=Math.max(1, b+delta);
  const fmt=n=>{
    // wenn Ganzzahl → ohne .0
    const isInt=Math.abs(n-Math.round(n))<1e-9;
    return (isInt? String(Math.round(n)) : String(Math.round(n*10)/10).replace('.',','));
  };
  return `${fmt(na)}–${fmt(nb)}${tail}`;
}

function planLevel(p){
  // 1) Grundlage: explizit gewähltes Trainingslevel
  let lvl = (p && p.experience) ? String(p.experience) : "new";
  if(lvl!=="new" && lvl!=="some" && lvl!=="pro") lvl="new";

  // 2) Sicherheits-Downgrade (nur nach unten, niemals nach oben!)
  const {age,bmi}=calcMetrics(p);
  if(age!=null && age>=50) lvl="new";
  if(bmi!=null && bmi>=32) lvl="new";

  return lvl;
}

function repRange(p,cat){
  const lvl=planLevel(p);
  const {bmi}=calcMetrics(p);
  const fatLoss = (p.mainGoal==="lose") || (bmi!=null && bmi>=28);
  const build = (p.mainGoal==="build");

  // Basiswerte
  let base;
  if(cat==="grip") base = fatLoss ? "40–70 Sek" : "30–60 Sek";
  else if(cat==="core") base = fatLoss ? "25–45 Sek" : "20–40 Sek";
  else if(build && !fatLoss){
    // Muskelaufbau: etwas niedrigere Wiederholungen
    if(lvl==="new") base = "6–8";
    else if(lvl==="some") base = "6–10";
    else base = "8–12";
  }
  else if(lvl==="new") base = fatLoss ? "10–14" : "6–10";
  else if(lvl==="some") base = fatLoss ? "10–15" : "8–12";
  else base = fatLoss ? "12–16" : "8–12";

  // Sichtbare Anpassung durch Gewicht/Alter (Score)
  // delta in Wiederholungen/Sekunden: -2..+2 (bei Zeitblöcken etwas größer)
  const sc = loadScore(p);
  let delta = 0;
  if(sc>=2) delta = 2;
  else if(sc===1) delta = 1;
  else if(sc===-1) delta = -1;
  else if(sc<=-2) delta = -2;

  // Zeitblöcke etwas stärker skalieren
  const isSec = /Sek/.test(base);
  const d = isSec ? delta*5 : delta;

  return adjustRange(base, d);
}

function setAppVisible(done){
  const h=document.querySelector("header.app");
  const m=document.querySelector("main");
  if(done){
    document.body.classList.add('appDark');
    h.style.display="block";
    m.style.display="block";
    $("#onboarding").style.display="none";
    $("#obCta").style.display="none";
  }else{
    document.body.classList.remove('appDark');
    h.style.display="none";
    m.style.display="none";
    $("#onboarding").style.display="";
    $("#obCta").style.display="";
  }
}

// Animation-Mapping (offline JSON). Wenn nichts lädt, nutzen wir Defaults.
// Regeln liefern jetzt: {match, type, anim}
const ANIM_RULES = {"rules": [{"match": "(crunch|sit-?up|bauchcrunch|ab crunch)", "type": "core", "anim": "crunch", "desc": "Crunch: Rippen Richtung Becken ziehen, kontrolliert aufrollen, Nacken lang.", "cues": ["Ausatmen beim Hochrollen", "Nicht am Nacken ziehen", "Spannung halten, langsam abrollen"]}, {"match": "(dips|bankdips|barrenstütz)", "type": "push", "anim": "dips", "desc": "Dips: Kontrolliert absenken und hochdrücken, Schultern unten halten.", "cues": ["Schultern weg von den Ohren", "Ellbogen kontrolliert, nicht ausflaren", "Schmerzfrei arbeiten"]}, {"match": "(kabelrudern|seated row|maschinenrudern)", "type": "pull", "anim": "row", "desc": "Kabel-/Maschinenrudern: Zieh zum Bauch, Schulterblätter aktiv nach hinten/unten, kontrolliert zurück.", "cues": ["Brust hoch, Rücken neutral", "Ellbogen nah am Körper", "Endposition kurz halten"]}, {"match": "(trizeps|pushdown|triceps pushdown|kabel.*trizeps|seil.*trizeps)", "type": "push", "anim": "triceps", "desc": "Trizepsdrücken am Kabel: Ellbogen fix, nach unten strecken, kontrolliert zurück.", "cues": ["Ellbogen am Körper fest", "Unten strecken ohne Schultern hochzuziehen", "Langsam zurück bis ~90°"]}, {"match": "(bizeps|curl|curls|hammercurl|hammer curl)", "type": "pull", "anim": "curl", "desc": "Bizepscurls: Oberarme ruhig am Körper, kontrolliert hochcurlen und langsam ablassen.", "cues": ["Ellbogen bleiben neben dem Körper", "Keine Hüft-/Rücken-Schwünge", "Oben kurz anspannen, unten Spannung halten"]}, {"match": "(kreuzheben|deadlift|rum(än|ae)nisches|romanian deadlift|rdl|hip hinge)", "type": "legs", "anim": "hinge", "desc": "Hip-Hinge/Kreuzheben: Hüfte nach hinten schieben, Rücken neutral, Gewicht nah am Körper führen.", "cues": ["Spannung im Rumpf, Schulterblätter unten", "Bewegung aus der Hüfte – Knie nur leicht beugen", "Langsam ablassen, über Fersen/Mittelfuß aufrichten"]}, {"match": "(beinpresse|leg press)", "type": "legs", "anim": "legpress", "desc": "Beinpresse: Füße stabil auf die Plattform, kontrolliert beugen und kraftvoll strecken ohne Knie zu blockieren.", "cues": ["Becken/Rücken an der Lehne lassen", "Knie folgen den Zehen, nicht nach innen", "Unten kurz stoppen, dann gleichmäßig hochdrücken"]}, {"match": "(kniebeuge|squat|goblet squat|smith squat)", "type": "legs", "anim": "squat", "desc": "Kniebeugen: Hüfte nach hinten/unten, Knie folgen den Zehen, Rücken bleibt neutral.", "cues": ["Füße schulterbreit, Druck über ganze Fußsohle", "Knie nach außen führen (nicht einknicken)", "Tief nur so weit, wie du stabil bleibst – dann sauber hoch"]}, {"match": "(schulterdr(ück|ueck)en|overhead press|military press|arnold press)", "type": "push", "anim": "overheadpress", "desc": "Schulterdrücken: Gewicht über Kopf drücken, Rippen unten halten, kontrolliert wieder absenken.", "cues": ["Po & Bauch anspannen (kein Hohlkreuz)", "Ellenbogen leicht vor dem Körper, Handgelenke gerade", "Oben Kopf „durch die Arme“, dann langsam zurück"]}, {"match": "(klimmz(ug|üge|uege).*(assist|maschine)|assist(iert|ed).*(pull|klimm))", "type": "pull", "anim": "pullup", "desc": "Assistierte Klimmzüge (Maschine/Band): Gleiche Technik wie beim normalen Klimmzug, nur mit Unterstützung.", "cues": ["Unterstützung so wählen, dass 8–12 saubere Wdh. möglich sind", "Zug aus dem Rücken (Ellbogen nach unten/hinten)", "Kontrolliert ablassen – keine „Fallenlassen“-Wdh."]}, {"match": "(klimmz(ug|üge|uege)|pull-?up|chin-?up|dead\s*hang)", "type": "pull", "anim": "pullup", "desc": "Klimmzüge: Starte aus dem stabilen Hang, zieh Brust Richtung Stange und senke kontrolliert ab.", "cues": ["Schulter zuerst aktivieren (Schultern runter, Brust hoch)", "Rumpf fest, kein Kippen/Schwung", "Oben kurz halten, dann langsam ablassen"]}, {"match": "(liegestütze|push[- ]?up)", "type": "push", "anim": "pushup", "desc": "Liegestütze: Körper wie ein Brett, kontrolliert absenken und kraftvoll hochdrücken.", "cues": ["Hände unter Schulter", "Rumpf fest (kein Hohlkreuz)", "Brust Richtung Boden, Ellbogen ca. 30–45°"]}, {"match": "(bankdrücken|kurzhantel-?bank|schrägbank|floor press)", "type": "push", "anim": "benchpress", "desc": "Bankdrücken: Hantel kontrolliert zur Brust, dann sauber hochdrücken.", "cues": ["Schulterblätter nach hinten/unten", "Füße fest am Boden", "Stange senkrecht hoch/runter"]}, {"match": "(butterfly|fliegende|pec-?deck|fly)", "type": "push", "anim": "fly", "desc": "Butterfly/Flys: Arme leicht gebeugt, kontrolliert öffnen und zusammenführen.", "cues": ["Nicht überstrecken", "Spannung auf der Brust halten", "Langsam, ohne Schwung"]}, {"match": "(rudern|row|türrudern)", "type": "pull", "anim": "row", "desc": "Rudern: Zug zum Bauch/untere Brust, Schulterblätter aktiv nach hinten ziehen.", "cues": ["Brust raus, Rücken neutral", "Ellbogen nah am Körper", "Am Ende kurz halten"]}, {"match": "(latzug|band-?latzug|bandzug von oben|maschinen-?latzug|lats)", "type": "pull", "anim": "latpulldown", "desc": "Latzug: Stange zur oberen Brust ziehen, kontrolliert wieder hochlassen.", "cues": ["Brust hoch, nicht nach hinten reißen", "Schultern unten lassen", "Kein Schwung"]}, {"match": "(klimmzug|pull-?up|dead hang)", "type": "pull", "anim": "pullup", "desc": "Klimmzug: Aus dem Hang hochziehen bis Kinn über die Stange, dann kontrolliert ablassen.", "cues": ["Rumpf fest", "Schulter zuerst aktivieren", "Saubere Wiederholungen vor Gewicht"]}, {"match": "(face pull)", "type": "pull", "anim": "facepull", "desc": "Face Pull: Band/Seil Richtung Gesicht ziehen, Ellbogen hoch, Schulterblattkontrolle.", "cues": ["Ellbogen hoch führen", "Endposition: Hände neben Gesicht", "Leicht, sauber, Schulterfreundlich"]}, {"match": "(plank)", "type": "core", "anim": "plank", "desc": "Plank: Unterarme am Boden, Körperlinie gerade, Spannung im Bauch/Po.", "cues": ["Bauch anspannen", "Po leicht anspannen", "Nacken neutral"]}, {"match": "(seitstütz|side plank)", "type": "core", "anim": "sideplank", "desc": "Seitstütz: Seitlich auf Unterarm, Hüfte hoch, Körperlinie gerade.", "cues": ["Hüfte nicht absinken lassen", "Schulter weg vom Ohr", "Atmen – nicht verkrampfen"]}], "default": {"type": "core", "anim": "generic", "desc": "Beispiel-Animation: Achte auf saubere Kontrolle und ruhige Wiederholungen.", "cues": ["Langsam & sauber", "Kein Schwung", "Schmerzfrei trainieren"]}};
function pickAnimDef(str){
  const s=(str||"").toLowerCase();
  const rules=(ANIM_RULES&&ANIM_RULES.rules)||[];
  for(const rule of rules){
    try{
      const re=new RegExp(rule.match,'i');
      if(re.test(s)) return {
        type: rule.type||'core',
        anim: rule.anim||'generic',
        desc: rule.desc||"",
        cues: Array.isArray(rule.cues)? rule.cues : []
      };
    }catch(e){}
  }
  const d=(ANIM_RULES&&ANIM_RULES.default)||{type:'core',anim:'generic',desc:"",cues:[]};
  return {
    type:d.type||'core',
    anim:d.anim||'generic',
    desc:d.desc||"",
    cues:Array.isArray(d.cues)? d.cues : []
  };
}

function typeLabel(t){
  return ({push:'Druck',pull:'Zug',legs:'Beine',core:'Core',mob:'Mobilität'}[t]||'Übung');
}

function renderAnimSVG(anim){
  // Stick-Figure SVGs: pro Übung eine Beispiel-Animation (Strichmännchen)
  // Animiert per CSS über Klassen.
  const frame=`<rect x="10" y="10" width="340" height="60" rx="16" fill="rgba(255,255,255,.05)" stroke="rgba(255,255,255,.10)"/>`;
  const defs=`<defs>
    <linearGradient id="g" x1="0" x2="1">
      <stop offset="0" stop-color="rgba(255,176,0,.95)"/>
      <stop offset="1" stop-color="rgba(255,106,0,.95)"/>
    </linearGradient>
  </defs>`;

  const stickBase=(extra)=>`<svg class="animSvg stick ex-${anim}" viewBox="0 0 360 80" role="img" aria-label="Übungsanimation">
    ${defs}
    ${frame}
    <g class="stickman" transform="translate(0,0)">
      ${extra}
    </g>
  </svg>`;

  // Gemeinsame Strichmännchen-Teile (Kopf/Körper/Beine) je Übung leicht angepasst.
  const head=(x,y)=>`<circle class="head" cx="${x}" cy="${y}" r="8" fill="rgba(255,255,255,.18)" stroke="rgba(255,255,255,.55)" stroke-width="2"/>`;
  const line=(cls,x1,y1,x2,y2,w=3)=>`<line class="${cls}" x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="rgba(255,255,255,.65)" stroke-width="${w}" stroke-linecap="round"/>`;
  const bar=(x,y,w)=>`<g class="bar" transform="translate(${x} ${y})"><rect x="${-w/2}" y="-4" width="${w}" height="8" rx="4" fill="url(#g)"/><rect x="${-w/2-16}" y="-12" width="16" height="24" rx="6" fill="url(#g)"/><rect x="${w/2}" y="-12" width="16" height="24" rx="6" fill="url(#g)"/></g>`;

  // Bench Press (Bankdrücken)
  if(anim==='benchpress'){
    return stickBase(`
      ${head(150,26)}
      ${line('body',150,34,150,52)}
      ${line('leg1',150,52,138,64)}
      ${line('leg2',150,52,162,64)}
      ${line('armL',150,40,130,48)}
      ${line('armR',150,40,170,48)}
      ${bar(240,38,110)}
      <rect x="105" y="56" width="110" height="6" rx="3" fill="rgba(255,255,255,.10)"/>
    `);
  }

  // Push-up (Liegestütze)
  if(anim==='pushup'){
    return stickBase(`
      <g class="pu">
        ${head(120,34)}
        ${line('torso',132,38,178,44,3)}
        ${line('leg1',178,44,210,56,3)}
        ${line('leg2',178,44,210,60,3)}
        ${line('armL',140,42,150,58,3)}
        ${line('armR',150,43,160,58,3)}
        <line class="floor" x1="90" y1="62" x2="270" y2="62" stroke="rgba(255,255,255,.18)" stroke-width="4" stroke-linecap="round"/>
      </g>
    `);
  }

  // Row (Rudern)
  if(anim==='row'){
    return stickBase(`
      <g class="rw">
        ${head(140,26)}
        ${line('body',140,34,154,54)}
        ${line('leg1',154,54,136,64)}
        ${line('leg2',154,54,170,64)}
        ${line('armL',148,42,190,38)}
        ${line('armR',150,44,192,44)}
        <g class="handle" transform="translate(235 41)">
          <circle cx="0" cy="0" r="6" fill="url(#g)"/>
          <path d="M0,0 C-20,-10 -35,0 -55,-6" fill="none" stroke="rgba(255,255,255,.25)" stroke-width="5" stroke-linecap="round"/>
        </g>
      </g>
    `);
  }

  // Lat Pulldown (Latzug)
  if(anim==='latpulldown'){
    return stickBase(`
      <g class="lp">
        ${head(170,26)}
        ${line('body',170,34,170,54)}
        ${line('leg1',170,54,156,64)}
        ${line('leg2',170,54,184,64)}
        ${line('armL',170,38,152,46)}
        ${line('armR',170,38,188,46)}
        <g class="bar" transform="translate(170 18)">
          <rect x="-70" y="-4" width="140" height="8" rx="4" fill="url(#g)"/>
        </g>
        <line x1="170" y1="18" x2="170" y2="30" stroke="rgba(255,255,255,.18)" stroke-width="4" stroke-linecap="round"/>
      </g>
    `);
  }

  // Pull-up (Klimmzug)
  if(anim==='pullup'){
    return stickBase(`
      <g class="pu2">
        <rect x="90" y="16" width="180" height="8" rx="4" fill="url(#g)"/>
        ${head(180,36)}
        ${line('body',180,44,180,58)}
        ${line('leg1',180,58,168,66)}
        ${line('leg2',180,58,192,66)}
        ${line('armL',180,44,140,24)}
        ${line('armR',180,44,220,24)}
      </g>
    `);
  }

  
  // Warm-up (Aufwärmen) – Jumping Jacks (Mobilität/Herz-Kreislauf)
  if(anim==='warmup'){
    return stickBase(`
      <g class="wu">
        ${head(180,22)}
        ${line('body',180,30,180,52)}
        ${line('armL armL',180,34,140,22)}
        ${line('armR armR',180,34,220,22)}
        ${line('legL legL',180,52,160,66)}
        ${line('legR legR',180,52,200,66)}
        <line class="floor" x1="90" y1="70" x2="270" y2="70" stroke="rgba(255,255,255,.14)" stroke-width="4" stroke-linecap="round"/>
      </g>
    `);
  }

// Fly (Butterfly / Flys)
  if(anim==='fly'){
    return stickBase(`
      <g class="fl">
        ${head(180,24)}
        ${line('body',180,32,180,54)}
        ${line('leg1',180,54,166,64)}
        ${line('leg2',180,54,194,64)}
        ${line('armL',180,40,140,30)}
        ${line('armR',180,40,220,30)}
        <circle class="dL" cx="140" cy="30" r="6" fill="url(#g)"/>
        <circle class="dR" cx="220" cy="30" r="6" fill="url(#g)"/>
      </g>
    `);
  }

  // Face Pull
  if(anim==='facepull'){
    return stickBase(`
      <g class="fp">
        ${head(180,24)}
        ${line('body',180,32,180,54)}
        ${line('leg1',180,54,166,64)}
        ${line('leg2',180,54,194,64)}
        ${line('armL',180,40,150,34)}
        ${line('armR',180,40,210,34)}
        <path class="band" d="M120,34 C150,34 210,34 240,34" fill="none" stroke="rgba(255,255,255,.25)" stroke-width="6" stroke-linecap="round"/>
        <circle cx="120" cy="34" r="6" fill="url(#g)"/>
        <circle cx="240" cy="34" r="6" fill="url(#g)"/>
      </g>
    `);
  }

  // Plank
  if(anim==='plank'){
    return stickBase(`
      <g class="pl">
        ${head(120,40)}
        ${line('torso',132,44,190,48,3)}
        ${line('leg1',190,48,220,60,3)}
        ${line('leg2',190,48,220,64,3)}
        ${line('armL',144,46,154,60,3)}
        <line class="floor" x1="90" y1="64" x2="270" y2="64" stroke="rgba(255,255,255,.18)" stroke-width="4" stroke-linecap="round"/>
      </g>
    `);
  }

  // Side Plank
  if(anim==='sideplank'){
    return stickBase(`
      <g class="sp">
        ${head(140,34)}
        ${line('torso',150,38,210,38,3)}
        ${line('leg1',210,38,240,50,3)}
        ${line('armL',160,38,150,56,3)}
        <line class="floor" x1="90" y1="60" x2="270" y2="60" stroke="rgba(255,255,255,.18)" stroke-width="4" stroke-linecap="round"/>
      </g>
    `);
  }

  // Default
  return stickBase(`
    ${head(180,24)}
    ${line('body',180,32,180,54)}
    ${line('leg1',180,54,166,64)}
    ${line('leg2',180,54,194,64)}
    ${line('armL',180,40,150,46)}
    ${line('armR',180,40,210,46)}
  `);
}


function readQ(){
  const q=questions[idx];
  const v = profile[q.id];
  if(v==null) return;
  profile.experience = (answers.experience || profile.experience || 'new');
  save(KEY_PROFILE, profile);
}

function stepMeta(i){
  // 01 ZIEL UND FOKUS / 02 ÜBER DEINEN KÖRPER / 03 PHYSISCHE BEWERTUNG
  const groups=[
    {from:0,to:4,label:"ZIEL UND FOKUS"},
    {from:5,to:10,label:"ÜBER DEINEN KÖRPER"},
    {from:11,to:questions.length-1,label:"PHYSISCHE BEWERTUNG"},
  ];
  const g=groups.find(x=>i>=x.from && i<=x.to) || groups[0];
  const stepNr = String(i+1).padStart(2,"0");
  return {stepNr,label:g.label};
}

function renderObProgress(){
  const wrap=$("#obProgress");
  wrap.innerHTML="";
  const segs = Math.max(4, Math.min(6, questions.length));
  for(let i=0;i<segs;i++){
    const seg=document.createElement("div");
    seg.className="obProgSeg";
    const bar=document.createElement("i");
    const from = (i/segs)*questions.length;
    const to = ((i+1)/segs)*questions.length;
    const cur = idx+1;
    let w=0;
    if(cur<=from) w=0;
    else if(cur>=to) w=100;
    else w = Math.round(((cur-from)/(to-from))*100);
    bar.style.width=w+"%";
    seg.appendChild(bar);
    wrap.appendChild(seg);
  }
}

function setObNextEnabled(enabled){
  const btn = $("#obNext");
  btn.disabled = !enabled;
  btn.textContent = (idx===questions.length-1) ? "Plan erstellen" : "NÄCHSTE";
  btn.className = "obBtn "+(enabled?"ghost":"gray");
}

function renderObQuestion(){
  const q=questions[idx];
  const meta=stepMeta(idx);
  $("#obStepNr").textContent=meta.stepNr;
  $("#obStepTxt").textContent=meta.label;
  $("#obTitle").textContent=q.title;
  $("#obHint").textContent=q.hint || "";
  renderObProgress();

  const opts=$("#obOptions");
  opts.innerHTML="";
  const current = profile[q.id] ?? null;

  // --- Renderer je nach Typ ---
  if(q.type==="number"){
    const wrap=document.createElement("div");
    wrap.className="obNumWrap";
    const input=document.createElement("input");
    input.type="number";
    input.className="obNum";
    input.min=String(q.min ?? "");
    input.max=String(q.max ?? "");
    input.step=String(q.step ?? "1");
    input.placeholder="";
    input.value = (current!=null && current!=="") ? String(current) : "";
    const unit=document.createElement("div");
    unit.className="obNumUnit";
    unit.textContent = q.unit || "";
    wrap.appendChild(input);
    wrap.appendChild(unit);

    const row=document.createElement("div");
    row.className="obNumBtns";
    const dec=document.createElement("button");
    dec.type="button"; dec.className="obMini"; dec.textContent="−";
    const inc=document.createElement("button");
    inc.type="button"; inc.className="obMini"; inc.textContent="+";
    row.appendChild(dec); row.appendChild(inc);

    const clamp=(v)=>{
      if(v===""||v==null) return "";
      let n=Number(v);
      if(Number.isNaN(n)) return "";
      if(q.min!=null) n=Math.max(q.min,n);
      if(q.max!=null) n=Math.min(q.max,n);
      // runden auf step
      const step=Number(q.step||1);
      if(step && step!==1){
        n=Math.round(n/step)*step;
        n=Number(n.toFixed(String(step).includes(".")?String(step).split(".")[1].length:0));
      }
      return n;
    };

    const sync=()=>{
      const val=clamp(input.value);
      if(val===""){
        delete profile[q.id];
      }else{
        profile[q.id]=val;
      }
      save(KEY_PROFILE, profile);
      const has = (val !== "");
      setObNextEnabled(has);
    };

    input.addEventListener("input", sync);
    input.addEventListener("change", ()=>{ input.value=String(clamp(input.value)); sync(); });

    const bump=(dir)=>{
      let v = (input.value===""? (q.min??0) : Number(input.value));
      const step=Number(q.step||1);
      v = v + dir*step;
      input.value = String(clamp(v));
      sync();
    };
    dec.addEventListener("click", ()=>bump(-1));
    inc.addEventListener("click", ()=>bump(1));

    opts.appendChild(wrap);
    opts.appendChild(row);

    // Initiale Validierung (wichtig für +/− und vorbefüllte Werte)
    sync();

  } else if(q.type==="range"){
    const wrap=document.createElement("div");
    wrap.className="obRangeWrap";
    const range=document.createElement("input");
    range.type="range";
    range.min=String(q.min ?? 1);
    range.max=String(q.max ?? 5);
    range.step=String(q.step ?? 1);
    const val = (current!=null && current!=="") ? Number(current) : Number(range.min);
    range.value=String(val);

    const labels=document.createElement("div");
    labels.className="obRangeLabels";
    labels.innerHTML = `<span>${q.leftLabel||""}</span><span>${q.rightLabel||""}</span>`;

    const valuePill=document.createElement("div");
    valuePill.className="obRangePill";
    valuePill.textContent = range.value;

    const setVal=()=>{
      profile[q.id]=Number(range.value);
      save(KEY_PROFILE, profile);
      valuePill.textContent=String(range.value);
      setObNextEnabled(true);
    };
    range.addEventListener("input", setVal);
    setVal();

    wrap.appendChild(valuePill);
    wrap.appendChild(range);
    wrap.appendChild(labels);
    opts.appendChild(wrap);

  } else {
    // choice (Standard)
    q.opts.forEach(([val, text])=>{
      const btn=document.createElement("button");
      btn.type="button";
      btn.className="obOpt"+(current===val?" sel":"");
      btn.innerHTML=`<div><strong>${text}</strong></div><div class="tick">✓</div>`;
      btn.addEventListener("click", ()=>{
        profile[q.id]=val;
        save(KEY_PROFILE, profile);
        renderObQuestion();
        setObNextEnabled(true);
      });
      opts.appendChild(btn);
    });
  }

  // Next-Button Zustand
  const has = profile[q.id]!=null && profile[q.id]!=="";
  setObNextEnabled(has);
  // Back-Button sichtbar
  $("#obBack").style.visibility = (idx===0) ? "hidden" : "visible";
}

function showWelcome(){
  $("#obWelcome").style.display="";
  $("#obQuestion").style.display="none";
  $("#obNext").disabled=false;
  $("#obNext").textContent="Ich bin bereit!";
  $("#obNext").className="obBtn ghost";
}

function showQuestions(){
  $("#obWelcome").style.display="none";
  $("#obQuestion").style.display="";
  renderObQuestion();
}

const LIB={
 warmup:["5 Min Fahrrad/Walking","Schultern kreisen","Rücken locker bewegen","Hüfte bewegen"],
 cooldown:["Dehnen: Brust/Rücken/Schultern","Ruhig atmen"],
 pull:"Latzug ODER Klimmzüge",
 row:"Klimmzüge (Alternative)",
 bench:"Bankdrücken (oder Liegestütze)",
 fly:"Butterfly (oder Liegestütze)",
 face:"Face Pull / Hintere Schulter",
 grip:"Farmer Hold (Griff)",
 plank:"Plank",
 side:"Seitstütz"
};

// Übungs-Alternativen: gleicher Zweck, unterschiedliche Optionen je nach Equipment
function exOptions(cat, p){
  const eq=p.equipment, pain=p.pain;
  const O=(...a)=>a.filter(Boolean);
  if(cat==="pull"){
    if(eq==="minimal") return O("Klimmzüge (Türstange) / Negativ-Klimmzüge","Inverted Rows (unter Tisch / stabiler Stab)","Bandzug von oben (wenn Band da)");
    if(eq==="home") return O("Klimmzüge (Türstange)","Inverted Rows (TRX/Band/unter Tisch)","Band-Latzug / Bandzug von oben");
    return O("Latzug (weiter/neutraler Griff)","Klimmzüge (Assistiert)","Maschinen-Latzug");
  }
  if(cat==="row"){
    // Kein "Rudern" verfügbar → gleiche Funktion (Rücken/Zug) über Klimmzug-Varianten.
    if(eq==="minimal") return O(
      "Klimmzüge (Türstange)",
      "Klimmzüge (Negativ: langsam ablassen)",
      "Inverted Rows (unter Tisch / stabiler Stab)"
    );
    if(eq==="home") return O(
      "Klimmzüge (Türstange)",
      "Klimmzüge (Assistiert mit Band)",
      "Inverted Rows (TRX/Band/unter Tisch)"
    );
    return O(
      "Klimmzüge (Assistiert)",
      "Klimmzüge (weiter/neutraler Griff)",
      "Latzug (Alternative wenn vorhanden)"
    );
  }
  if(cat==="push"){
    const shoulderFriendly = pain==="shoulder";
    if(eq==="minimal") return shoulderFriendly
      ? O("Liegestütze erhöht (Hände höher)","Schulter-freundliche Liegestütze (eng/kurz)","Dips am Stuhl (klein)")
      : O("Liegestütze","Liegestütze (eng)","Dips am Stuhl");
    if(eq==="home") return shoulderFriendly
      ? O("Schrägbankdrücken (Kurzhanteln, leicht)","Floor Press (Kurzhanteln)","Liegestütze")
      : O("Bankdrücken (Kurzhanteln)","Floor Press (Kurzhanteln)","Liegestütze");
    return shoulderFriendly
      ? O("Schrägbank (leicht)","Maschinen-Drücken (neutral)","Kurzhantel-Bankdrücken (leicht)")
      : O("Bankdrücken (Hantelbank)","Kurzhantel-Bankdrücken","Maschinen-Drücken");
  }
  if(cat==="chestExtra"){
    if(eq==="minimal") return O("Liegestütze (langsamer)","Liegestütze (Pause unten)","Liegestütze (breit)");
    if(eq==="home") return O("Fliegende (Kurzhanteln)","Liegestütze","Kurzhantel-Bankdrücken leicht");
    return O("Butterfly / Pec-Deck","Kabelzug-Flys","Liegestütze");
  }
  if(cat==="balance"){
    if(eq==="minimal") return O("Y-T-W (am Boden)","Band Face Pull","Reverse Fly (Wasserflaschen)");
    if(eq==="home") return O("Band Face Pull","Reverse Fly (Kurzhanteln)","Seitheben leicht");
    return O("Face Pull (Kabel)","Reverse Fly Maschine","Seitheben (leicht)");
  }
  if(cat==="grip"){
    if(eq==="minimal") return O("Handtuch-Hängen (wenn möglich)","Farmer Hold (Einkaufstaschen)","Unterarm-Curls (Wasserflaschen)");
    if(eq==="home") return O("Farmer Hold (Kurzhanteln)","Handtuch-Hängen (Türstange)","Wrist Curls (Kurzhanteln)");
    return O("Farmer Walk / Hold","Dead Hang (Klimmzugstange)","Gripper / Unterarm-Rolle");
  }
  if(cat==="core"){
    return O("Plank","Seitstütz","Dead Bug");
  }
  return O("(Option)");
}

function setsBase(exp){
  if(exp==="new") return {main:2, extra:1, core:1};
  if(exp==="some") return {main:3, extra:2, core:2};
  return {main:4, extra:2, core:2};
}
function targetMin(p){
  let t=Number(p.timePref||45);
  if(p.workload==="high") t-=10;
  if(p.workload==="low") t+=5;
  if(p.focus==="mobility") t=Math.max(25, t-5);
  return Math.max(25, Math.min(95, t));
}
function buildPlan(p){
  const t=targetMin(p);
  const lvl=planLevel(p);
  // Sätze werden jetzt aus ALLEN Start-Infos berechnet (Alter/BMI/Ziel/Körperform/Workload)
  const s=setsPlan(p);
  const baseRest = lvl==="new" ? 90 : (lvl==="some" ? 75 : 60);
// Score: schwerer/älter → mehr Pause, leichter/jünger → weniger Pause
const sc = loadScore(p);
const rest = Math.max(30, Math.min(150, baseRest + (-sc*10)));
  const focus=p.focus;
  const {age,bmi}=calcMetrics(p);
  const needsMoreWarmup = (age!=null && age>=45) || (bmi!=null && bmi>=28);

  const plan=[];
  plan.push({type:"warmup", title:"Aufwärmen", min:(needsMoreWarmup?12:10), items:LIB.warmup});

  // pick exercises (+ Alternativen mit gleichem Zweck)
  const pullOpts = exOptions("pull", p);
  const rowOpts  = exOptions("row", p);
  const pushOpts = exOptions("push", p);
  const chestOpts= exOptions("chestExtra", p);
  const balOpts  = exOptions("balance", p);
  const gripOpts = exOptions("grip", p);
  const coreOpts = exOptions("core", p);

  const list = (focus==="mobility")
    ? [
        {t:"Ziehen", cat:"pull", opts:pullOpts, sets:s.main, reps:repRange(p,"pull")},
        {t:"Rücken (Alternative)", cat:"row",  opts:rowOpts,  sets:s.main, reps:repRange(p,"row")},
        {t:"Balance",cat:"balance",opts:balOpts, sets:s.extra, reps:repRange(p,"balance")},
        {t:"Griff",  cat:"grip", opts:gripOpts, sets:s.extra, reps:repRange(p,"grip")},
        {t:"Core",   cat:"core", opts:coreOpts, sets:s.core, reps:repRange(p,"core")},
      ]
    : [
        {t:"Ziehen", cat:"pull", opts:pullOpts, sets:s.main, reps:repRange(p,"pull")},
        {t:"Drücken",cat:"push", opts:pushOpts, sets:s.main, reps:repRange(p,"push")},
        {t:"Brust extra",cat:"chestExtra",opts:chestOpts, sets:s.extra, reps:repRange(p,"chestExtra")},
        {t:"Rücken (Alternative)", cat:"row",  opts:rowOpts,  sets:s.extra, reps:repRange(p,"row")},
        {t:"Balance",cat:"balance",opts:balOpts, sets:s.extra, reps:repRange(p,"balance")},
        {t:"Griff",  cat:"grip", opts:gripOpts, sets:s.extra, reps:repRange(p,"grip")},
      ];

  list.forEach(x=>plan.push({type:"ex", title:x.t, cat:x.cat, ex:x.opts[0], alts:x.opts.slice(1), sets:x.sets, reps:x.reps}));
  plan.push({type:"cooldown", title:"Cooldown", min:(focus==="mobility"?10:5), items:LIB.cooldown});

  // estimate
  let work=0;
  plan.forEach(b=>{
    if(b.type!=="ex") return;
    const per = (b.title==="Core") ? 1.5 : 2.5;
    work += b.sets*per + Math.max(0,(b.sets-1)*(rest/60));
  });
  const est=Math.round(10 + (p.focus==="mobility"?10:5) + work);

  return {createdAt:Date.now(), target:t, estTotal:est, restSec:rest, plan, profile:p};
}


// --- Onboarding Events ---
$("#obNext").addEventListener("click", ()=>{
  const done = load(KEY_ONBOARD,false);
  if($("#obWelcome").style.display!== "none"){
    idx=0;
    showQuestions();
    return;
  }
  // In Fragen
  const q=questions[idx];
  const has = profile[q.id]!=null && profile[q.id]!=="";
  if(!has) return;
  if(idx<questions.length-1){
    idx++;
    showQuestions();
    return;
  }
  // Fertig → Plan erstellen
  // Mapping: Antworten → Plan-Profile (grobe Heuristik)
  if(profile.job){
    if(profile.job==="shift") profile.workload="high";
    else if(profile.job==="fulltime" || profile.job==="owner") profile.workload="mid";
    else profile.workload="low";
  }
  if(profile.trainWhere){
    // Geräte-Übungen wieder aktivieren: Ort -> Equipment
    if(profile.trainWhere==="gym") profile.equipment = "gym";
    else if(profile.trainWhere==="any") profile.equipment = "home";
    else profile.equipment = "minimal";
  }
  if(profile.mainGoal){
        profile.focus = (profile.mainGoal==="build") ? "strength" : (profile.mainGoal==="fit" ? "balance" : "balance");
  }
  const plan=buildPlan(profile);
  save(KEY_PLAN, plan);
  save(KEY_ONBOARD, true);
  localStorage.removeItem(KEY_SESSION);
  setAppVisible(true);
  save(KEY_PROFILE, profile);
  renderProfileUI();
  document.querySelector('.tab[data-tab="plan"]').click();
  toast("Plan erstellt ✓");
});

$("#obBack").addEventListener("click", ()=>{
  if(idx>0){
    idx--;
    showQuestions();
  }else{
    showWelcome();
  }
});

// Beim Laden: wenn Plan schon existiert → direkt App, sonst Onboarding
(function boot(){
  const hasPlan = !!load(KEY_PLAN, null);
  if(hasPlan){
    save(KEY_ONBOARD, true);
    setAppVisible(true);
    // Profil aus dem gespeicherten Plan (falls vorhanden) übernehmen
    const p=load(KEY_PLAN,null)?.profile;
    if(p && typeof p==="object"){
      profile={...profile, ...p};
      save(KEY_PROFILE, profile);
    }
    renderProfileUI();
  }else{
    setAppVisible(false);
    showWelcome();
  }
})();

// Profil-Buttons
$("#saveProfile").addEventListener("click", ()=>{
  profile.mainGoal = $("#pf_mainGoal").value;
  
  profile.experience = $("#pf_experience") ? $("#pf_experience").value : (profile.experience||"new");
profile.birthYear = $("#pf_birthYear").value;
  profile.heightCm = num($("#pf_height").value);
  profile.weightKg = num($("#pf_weight").value);
  profile.targetWeightKg = num($("#pf_targetWeight").value);
  profile.bodyFormNow = num($("#pf_bodyNow").value);
  profile.bodyFormGoal = num($("#pf_bodyGoal").value);
  save(KEY_PROFILE, profile);
  renderProfileUI();
  
  // Plan neu berechnen damit Level/Sätze/Wdh sofort wirken
  const plan=buildPlan(profile);
  save(KEY_PLAN, plan);
  localStorage.removeItem(KEY_SESSION);
  renderPlan();
toast("Profil gespeichert ✓");
});

$("#rebuildPlan").addEventListener("click", ()=>{
  // Plan komplett neu berechnen aus aktuellem Profil
  const plan=buildPlan(profile);
  save(KEY_PLAN, plan);
  localStorage.removeItem(KEY_SESSION);
  toast("Plan neu erstellt ✓");
  document.querySelector('.tab[data-tab="plan"]').click();
});

$("#backBtn").addEventListener("click", ()=>{
  if(idx===0){
    localStorage.removeItem(KEY_PROFILE);
    localStorage.removeItem(KEY_PLAN);
    localStorage.removeItem(KEY_SESSION);
    profile={experience:"new",workload:"mid",timePref:"45",equipment:"minimal",focus:"balance",pain:"none"};
    idx=0; renderQ(); toast("Reset ✓"); return;
  }
  readQ(); idx=Math.max(0, idx-1); renderQ();
});
$("#nextBtn").addEventListener("click", ()=>{
  readQ();
  if(idx<questions.length-1){ idx++; renderQ(); return; }
  const plan=buildPlan(profile);
  save(KEY_PLAN, plan);
  localStorage.removeItem(KEY_SESSION);
  document.querySelector('.tab[data-tab="plan"]').click();
  toast("Plan erstellt ✓");
});

function renderPlan(){
  const data=load(KEY_PLAN,null);
  if(!data){ document.querySelector('.tab[data-tab="setup"]').click(); return; }
  $("#planMeta").innerHTML = `<span class="pill"><b>Ziel:</b> ${data.target} Min</span> <span class="pill"><b>Geschätzt:</b> <span class="ok">${data.estTotal} Min</span></span> <span class="pill"><b>Pausen:</b> ${data.restSec} Sek</span>`;
  const ul=$("#planList"); ul.innerHTML="";
  let n=0;
  data.plan.forEach(b=>{
    const li=document.createElement("li");
    if(b.type==="warmup"){
      li.classList.add("exRow");
      li.setAttribute("data-openwarmup","1");
      li.innerHTML=`<div class="num">1</div><div><div class="k">${b.title} · ${b.min} Min</div><div class="small">${b.items.map(x=>"• "+x).join("<br>")}<br><span style="opacity:.85; font-weight:900">Tippen für Erklärung & Beispiel</span></div></div>`;
    }else if(b.type==="cooldown"){
      li.innerHTML=`<div class="num">3</div><div><div class="k">${b.title} · ${b.min} Min</div><div class="small">${b.items.map(x=>"• "+x).join("<br>")}</div></div>`;
    }else{
      n++;
      const alts=(b.alts && b.alts.length)
        ? `<br><b>Alternativen:</b> ${b.alts.map(x=>x).join(" · ")}`
        : "";
      li.classList.add("exRow");
      li.setAttribute("data-openexplan", String(n-1));
      li.innerHTML=`<div class="num">${n}</div><div><div class="k">${b.title}: ${b.ex}</div><div class="small"><b>Sätze:</b> ${b.sets} · <b>Wdh:</b> ${b.reps} · <b>Pause:</b> ${data.restSec} Sek${alts}</div></div>`;
    }
    ul.appendChild(li);
  });

  // Klick auf Plan-Übung öffnet Popup
  ul.querySelectorAll("li[data-openexplan]").forEach(li=>{
    li.addEventListener("click", ()=>{
      const idxEx=Number(li.getAttribute("data-openexplan"));
      const exBlocks=(data.plan||[]).filter(x=>x.type==="ex");
      const b=exBlocks[idxEx];
      if(!b) return;
      const def=pickAnimDef(b.ex);
      const sub=`${b.ex} · ${b.sets} Sätze · ${b.reps}`;
      openExerciseModal({
        title: b.title,
        sub,
        tag: typeLabel(def.type),
        anim: def.anim,
        ytQuery: b.ex,
        desc: def.desc||"",
        cues: def.cues||[]
      ,
        terms: (def.terms||TERMS_DEFAULT)
      });
    });

  // Klick auf Aufwärmen öffnet Erklärung + Beispiel
  ul.querySelectorAll('li[data-openwarmup]').forEach(li=>{
    li.addEventListener('click', ()=>{
      openExerciseModal({
        title: "Aufwärmen",
        sub: "10 Min · Gelenke vorbereiten · Körpertemperatur erhöhen",
        tag: "Mobilität",
        anim: "warmup",
        desc: "Aufwärmen bereitet Muskeln, Gelenke und Nervensystem vor. Du wirst beweglicher, fühlst dich stabiler und kannst die ersten Sätze sauberer ausführen. Starte leicht und steigere dich langsam – du sollst warm werden, aber nicht außer Atem geraten.",
        cues: [
          "1) 5 Min locker gehen / Fahrrad (Puls hochbringen): 1 Minute sehr locker starten, dann 3 Minuten leicht steigern, letzte Minute zügig – aber so, dass du noch reden kannst. Schultern locker, ruhig atmen.",
          "2) Schultern kreisen: 30 Sek vorwärts + 30 Sek rückwärts: Kreise groß & kontrolliert: hoch → hinten → runter → vorne. Kein Hektik-Tempo, Nacken entspannt, Kopf lang.",
          "3) Rücken & Hüfte mobilisieren: 6–10 Wdh: Cat-Cow im Vierfüßler (Brust öffnen ↔ Rücken rund). Danach Hip-Hinge: Po nach hinten schieben, Rücken lang/neutral, Knie leicht gebeugt.",
          "4) 1–2 leichte Aufwärmsätze der ersten Übung: Satz 1 sehr leicht (8–10 Wdh), Satz 2 moderat (5–8 Wdh). Kein Muskelversagen – Fokus auf Technik & kontrolliertes Tempo."
        ],
        ytLinks: [
          { label: "Punkt 1 – Puls hochbringen", query: "ATHLEAN-X warm up cardio" },
          { label: "Punkt 2 – Schultern mobilisieren", query: "ATHLEAN-X shoulder warm up" },
          { label: "Punkt 3 – Rücken & Hüfte", query: "ATHLEAN-X hip mobility warm up" },
          { label: "Punkt 4 – Aufwärmsätze", query: "ATHLEAN-X warm up sets" }
        ]
      ,
        terms: (def.terms||TERMS_DEFAULT)
      });
    });
  });

  });

  // session list
  // Session speichert jetzt pro Übung: {done:boolean, choice:number}
  const session=load(KEY_SESSION,{});
  const box=$("#sessionList"); box.innerHTML="";
  data.plan.filter(x=>x.type==="ex").forEach((b,i)=>{
    const id=`${i}`;
    const s=session[id];
    const checked=typeof s==="object" ? !!s.done : !!s;
    const choice=typeof s==="object" && Number.isFinite(s.choice) ? s.choice : 0;
    const card=document.createElement("div");
    card.className="card"; card.style.borderRadius="14px"; card.style.boxShadow="none"; card.style.margin="10px 0";
    const opts=[b.ex].concat(b.alts||[]);
    const chosenText=opts[choice]||opts[0]||"";
    const animDef=pickAnimDef(chosenText);
    card.innerHTML=`<div class="content">
      <label style="margin:0; display:flex; gap:10px; align-items:flex-start; color:var(--txt)">
        <input class="check" type="checkbox" ${checked?"checked":""} data-id="${id}">
        <div style="width:100%">
          <div style="display:flex; align-items:center; justify-content:space-between; gap:10px">
            <div class="k">${b.title}</div>
            <button class="infoMini" type="button" data-openex="${id}">Übung ansehen</button>
          </div>
          <div class="small" style="margin-top:8px">
            <b>Übung:</b>
            <select data-sel="${id}" style="margin-top:6px">
              ${opts.map((t,ix)=>`<option value="${ix}">${t}</option>`).join("")}
            </select>
          </div>
          <div class="small" style="margin-top:6px">${b.sets} Sätze · ${b.reps}</div>
        </div>
      </label>
    </div>`;
    box.appendChild(card);
    const sel=card.querySelector(`select[data-sel="${id}"]`);
    sel.value=String(choice);
  });
  box.querySelectorAll("input[type=checkbox]").forEach(cb=>{
    cb.addEventListener("change", ()=>{
      const id=cb.dataset.id;
      const prev=session[id];
      const choice=typeof prev==="object" && Number.isFinite(prev.choice) ? prev.choice : 0;
      session[id]={done:cb.checked, choice};
      save(KEY_SESSION, session);
    });
  });
  box.querySelectorAll("select[data-sel]").forEach(sel=>{
    sel.addEventListener("change", ()=>{
      const id=sel.getAttribute("data-sel");
      const prev=session[id];
      const done=typeof prev==="object" ? !!prev.done : !!prev;
      session[id]={done, choice: Number(sel.value||0)};
      save(KEY_SESSION, session);
      // Popup nutzt die Auswahl automatisch beim Öffnen.
    });
  });


// Übung ansehen (Popup) – App bleibt im Hintergrund sichtbar
box.querySelectorAll("button[data-openex]").forEach(btn=>{
  btn.addEventListener("click", (ev)=>{
    ev.preventDefault(); ev.stopPropagation();
    const id=btn.getAttribute("data-openex");
    const plan=load(KEY_PLAN,null);
    const exBlocks=(plan?.plan||[]).filter(x=>x.type==="ex");
    const b=exBlocks[Number(id)];
    if(!b) return;

    const session=load(KEY_SESSION,{});
    const s=session[id];
    const choice=(typeof s==="object" && Number.isFinite(s.choice)) ? s.choice : 0;
    const opts=[b.ex].concat(b.alts||[]);
    const chosen=opts[choice]||opts[0]||"";
    const def=pickAnimDef(chosen);

    openExerciseModal({
      title: b.title,
      sub: `${chosen} · ${b.sets} Sätze · ${b.reps}`,
      tag: typeLabel(def.type),
      anim: def.anim,
      ytQuery: chosen,
      desc: def.desc||"",
      cues: def.cues||[]
    ,
      terms: (def.terms||TERMS_DEFAULT)
    });
  });
});
}

$("#saveLog").addEventListener("click", ()=>{
  const plan=load(KEY_PLAN,null);
  if(!plan){ toast("Erst Plan erstellen"); return; }
  const date=$("#logDate").value || iso(new Date());
  const note=$("#logNote").value.trim();
  const session=load(KEY_SESSION,{});
  const done=Object.values(session).filter(v=> (typeof v==="object") ? !!v.done : !!v).length;
  const total=(plan.plan||[]).filter(x=>x.type==="ex").length;
  const entry={ts:Date.now(), date, note, done, total, target:plan.target, estTotal:plan.estTotal};
  const log=load(KEY_LOG,{});
  if(!log[date]) log[date]=[];
  log[date].push(entry);
  save(KEY_LOG, log);
  $("#logNote").value="";
  toast("Gespeichert ✓");
  renderHistory();
});

$("#resetSession").addEventListener("click", ()=>{
  localStorage.removeItem(KEY_SESSION);
  toast("Session reset");
  renderPlan();
});

function renderHistory(){
  const log=load(KEY_LOG,{});
  const all=[];
  Object.keys(log).forEach(d=>(log[d]||[]).forEach(e=>all.push(e)));
  all.sort((a,b)=>b.ts-a.ts);
  const last=all.slice(0,10);
  const box=$("#history");
  if(!last.length){ box.innerHTML='<div class="small">Noch keine Einträge.</div>'; return; }
  box.innerHTML = last.map(e=>{
    const dt=new Date(e.date).toLocaleDateString("de-DE");
    const note=(e.note||"").replace(/</g,"&lt;");
    return `<div class="card" style="border-radius:14px; box-shadow:none; margin:10px 0">
      <div class="content">
        <div class="k">${dt}</div>
        <div class="small"><b>Abgehakt:</b> ${e.done}/${e.total} · <b>Ziel:</b> ${e.target} Min</div>
        <div class="small">Notiz: ${note ? note : "<span style='opacity:.7'>(keine)</span>"}</div>
      </div>
    </div>`;
  }).join("");
}

function openExerciseModal(payload){
  // PATCH_EXERCISE_PAGE: statt Popup -> eigene Übungs-Seite (fullscreen, stabil in WebIntoApp)
  try{ sessionStorage.setItem('currentExercise', JSON.stringify(payload||{})); }catch(e){}
  window.location.href = 'exercise.html';
  return;
}

function closeExerciseModal(){
  const overlay = $("#exModal");
  overlay.classList.remove("show");
  overlay.setAttribute("aria-hidden","true");
}


document.addEventListener("click",(e)=>{
  const overlay=$("#exModal");
  if(overlay.classList.contains("show") && e.target===overlay) closeExerciseModal();
});
document.addEventListener("keydown",(e)=>{
  if(e.key==="Escape") closeExerciseModal();
});
$("#exModalClose").addEventListener("click", closeExerciseModal);

let toastTimer=null;

function toast(msg){
  let el=document.getElementById("toast");
  if(!el){
    el=document.createElement("div");
    el.id="toast";
    el.style.position="fixed";
    el.style.left="50%"; el.style.bottom="110px";
    el.style.transform="translateX(-50%)";
    el.style.padding="10px 12px";
    el.style.borderRadius="14px";
    el.style.border="1px solid rgba(255,255,255,.12)";
    el.style.background="rgba(0,0,0,.45)";
    el.style.backdropFilter="blur(10px)";
    el.style.color="white";
    el.style.fontWeight="900";
    el.style.boxShadow="0 16px 40px rgba(0,0,0,.45)";
    el.style.zIndex="60";
    document.body.appendChild(el);
  }
  el.textContent=msg; el.style.display="block";
  clearTimeout(toastTimer);
  toastTimer=setTimeout(()=>el.style.display="none", 1400);
}

const existing=load(KEY_PLAN,null);
if(existing){ document.querySelector('.tab[data-tab="plan"]').click(); renderHistory(); }
else { renderQ(); }

// PWA: Service Worker registrieren (für Installierbarkeit & Offline-Cache)
if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./sw.js").catch(()=>{});
  });
}


// PATCH_EXERCISE_PAGE_OVERRIDE: Jede Übung öffnet eine eigene Seite (exercise.html)
(function(){
  const _orig = (typeof openExerciseModal === "function") ? openExerciseModal : null;
  window.openExerciseModal = function(payload){
    try{ sessionStorage.setItem('currentExercise', JSON.stringify(payload||{})); }catch(e){}
    window.location.href = "exercise.html?ts=" + Date.now();
  };
})();
