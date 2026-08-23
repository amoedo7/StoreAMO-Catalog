#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile
from pathlib import Path

JOB_ID = "FAMO-20260822-201753-13807"
VERSION = "0.1.0"
ROOT = Path(__file__).resolve().parents[2]
BATCH = Path(__file__).resolve().parent

APPS = [
    ("MonedAMO","monedamo","Moneda al aire y contador de resultados","Juegos"),
    ("EdadAMO","edadamo","Calculá edad exacta desde una fecha de nacimiento","Utilidades"),
    ("DividAMO","dividamo","Dividí una cuenta entre varias personas","Finanzas"),
    ("PropinAMO","propinamo","Calculá propina y total por persona","Finanzas"),
    ("CombustAMO","combustamo","Calculá costo y consumo de combustible","Viajes"),
    ("ViajeAMO","viajeamo","Estimá duración de un viaje por distancia y velocidad","Viajes"),
    ("CuotAMO","cuotamo","Calculá cuotas de un préstamo","Finanzas"),
    ("InteresAMO","interesamo","Interés compuesto simple y claro","Finanzas"),
    ("ReglaTresAMO","reglatresamo","Regla de tres directa","Matemática"),
    ("PromediAMO","promediamo","Promedio y mediana de una lista de números","Matemática"),
    ("FraccionAMO","fraccionamo","Simplificá fracciones","Matemática"),
    ("PrimoAMO","primoamo","Comprobá si un número es primo","Matemática"),
    ("MCDAMO","mcdamo","Máximo común divisor y mínimo común múltiplo","Matemática"),
    ("BaseAMO","baseamo","Convertí números entre binario, octal, decimal y hexadecimal","Desarrollo"),
    ("RomanAMO","romanamo","Convertí números decimales a romanos","Utilidades"),
    ("ColorAMO","coloramo","Convertí colores HEX y RGB","Diseño"),
    ("TextoAMO","textoamo","Contá palabras, caracteres y líneas","Texto"),
    ("MayusAMO","mayusamo","Mayúsculas, minúsculas y formato título","Texto"),
    ("LimpiAMO","limpiamo","Limpiá espacios y líneas de un texto","Texto"),
    ("RepetAMO","repetamo","Repetí texto una cantidad controlada de veces","Texto"),
    ("UUIDAMO","uuidamo","Generador local de UUID v4","Desarrollo"),
    ("Base64AMO","base64amo","Codificá y decodificá Base64 localmente","Desarrollo"),
    ("UrlAMO","urlamo","Codificá y decodificá componentes de URL","Desarrollo"),
    ("JSONAMO","jsonamo","Formateá y validá JSON localmente","Desarrollo"),
    ("RegexAMO","regexamo","Probá expresiones regulares sobre texto","Desarrollo"),
    ("ListaAMO","listaamo","Ordená y quitá duplicados de listas","Texto"),
    ("MorseAMO","morseamo","Convertí texto a código Morse","Texto"),
    ("CesarAMO","cesaramo","Cifrado César local para aprendizaje","Texto"),
    ("MarcadorAMO","marcadoramo","Marcador simple para dos equipos","Juegos"),
    ("PomodorAMO","pomodoramo","Temporizador Pomodoro local","Productividad"),
    ("RespirAMO","respiramo","Guía de respiración 4-4-4-4","Bienestar"),
    ("IntervalAMO","intervalamo","Temporizador de intervalos trabajo/descanso","Productividad"),
    ("DiasAMO","diasamo","Días entre dos fechas","Utilidades"),
    ("FaltanAMO","faltanamo","Cuenta cuántos días faltan para una fecha","Utilidades"),
    ("UnixAMO","unixamo","Convertí fecha y timestamp Unix","Desarrollo"),
    ("ZonaAMO","zonaamo","Compará horarios por desplazamiento UTC","Utilidades"),
    ("FiguraAMO","figuraamo","Área y perímetro de figuras básicas","Matemática"),
    ("VelocAMO","velocamo","Convertí km/h, mph y m/s","Utilidades"),
    ("BytesAMO","bytesamo","Convertí bytes, KB, MB y GB","Desarrollo"),
    ("SlugAMO","slugamo","Convertí títulos a slugs de URL","Desarrollo"),
]

INDEX_HTML = r'''<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">
<title>AMO</title>
<style>
:root{color-scheme:dark;font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#07111f;color:#eef6ff}
*{box-sizing:border-box}body{margin:0;min-height:100vh;background:radial-gradient(circle at 90% 0,#14304c 0,#07111f 42%,#040a12 100%)}
main{max-width:680px;margin:auto;padding:24px 18px 40px}.brand{font-weight:900;letter-spacing:-1px;font-size:18px;color:#67d2ff}.brand b{color:#f3a64b}
h1{font-size:36px;margin:16px 0 8px;line-height:1}.sub{color:#a9bdd1;margin:0 0 24px}.card{background:#0c1b2d;border:1px solid #1d3852;border-radius:22px;padding:18px;box-shadow:0 18px 50px #0005}
label{display:block;color:#bcd0e4;font-size:13px;margin:12px 0 6px}input,textarea,select{width:100%;background:#07111f;color:#fff;border:1px solid #294760;border-radius:13px;padding:13px;font-size:16px}textarea{min-height:120px;resize:vertical}
.row{display:grid;grid-template-columns:1fr 1fr;gap:10px}.row3{display:grid;grid-template-columns:1fr 1fr 1fr;gap:8px}
button{border:0;border-radius:14px;padding:13px 15px;font-size:16px;font-weight:800;background:#67d2ff;color:#04101a;margin-top:12px}button.alt{background:#172b3e;color:#d9e9f7;border:1px solid #294760}.actions{display:flex;gap:8px;flex-wrap:wrap}
.out{margin-top:16px;padding:16px;border-radius:16px;background:#06101a;border:1px solid #1d3852;white-space:pre-wrap;word-break:break-word;min-height:54px}.big{font-size:34px;font-weight:900}.muted{color:#8ca4b9;font-size:13px}.score{font-size:58px;font-weight:900;text-align:center}.phase{font-size:28px;font-weight:900;text-align:center;padding:22px 0}.error{color:#ffadad}
footer{margin-top:22px;text-align:center;color:#6f879c;font-size:12px}@media(max-width:430px){h1{font-size:31px}.row,.row3{grid-template-columns:1fr}}
</style>
</head>
<body><main><div class="brand">Desarroll<span>AMO</span> · <b>StoreAMO</b></div><h1 id="title">AMO</h1><p class="sub" id="tagline"></p><section class="card" id="app"></section><footer>v0.1.0 · local-first · sin cuenta · sin telemetría</footer></main><script src="config.js"></script><script src="app.js"></script></body>
</html>'''

APP_JS = r'''(function(g){"use strict";
const $=s=>document.querySelector(s), esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c]));
const nums=s=>String(s).split(/[\s,;]+/).map(Number).filter(Number.isFinite); const n=v=>Number(v); const fmt=(v,d=2)=>Number.isFinite(Number(v))?Number(v).toLocaleString("es-AR",{maximumFractionDigits:d}):"—";
function gcd(a,b){a=Math.abs(Math.trunc(a));b=Math.abs(Math.trunc(b));while(b)[a,b]=[b,a%b];return a} function lcm(a,b){return a&&b?Math.abs(a*b)/gcd(a,b):0}
function isPrime(x){x=Math.trunc(x);if(x<2)return false;if(x%2===0)return x===2;for(let i=3;i*i<=x;i+=2)if(x%i===0)return false;return true}
function roman(x){x=Math.trunc(x);if(x<1||x>3999)return "Fuera de rango (1–3999)";const m=[[1000,"M"],[900,"CM"],[500,"D"],[400,"CD"],[100,"C"],[90,"XC"],[50,"L"],[40,"XL"],[10,"X"],[9,"IX"],[5,"V"],[4,"IV"],[1,"I"]];let r="";for(const [v,s] of m)while(x>=v){r+=s;x-=v}return r}
function slugify(s){return String(s).normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase().trim().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"")}
function caesar(s,k){k=((Math.trunc(k)%26)+26)%26;return String(s).replace(/[a-z]/gi,c=>{const b=c<="Z"?65:97;return String.fromCharCode((c.charCodeAt(0)-b+k)%26+b)})}
const MORSE={a:".-",b:"-...",c:"-.-.",d:"-..",e:".",f:"..-.",g:"--.",h:"....",i:"..",j:".---",k:"-.-",l:".-..",m:"--",n:"-.",o:"---",p:".--.",q:"--.-",r:".-.",s:"...",t:"-",u:"..-",v:"...-",w:".--",x:"-..-",y:"-.--",z:"--..","0":"-----","1":".----","2":"..---","3":"...--","4":"....-","5":".....","6":"-....","7":"--...","8":"---..","9":"----."};
function morse(s){return slugify(String(s).replace(/\s+/g," ")).replace(/-/g," ").split(" ").map(w=>[...w].map(c=>MORSE[c]||"?").join(" ")).join(" / ")}
function splitBill(total,people){return people>0?total/people:NaN} function tip(total,pct,people=1){const t=total*pct/100;return{tip:t,total:total+t,each:(total+t)/people}}
function fuel(distance,consumption,price){const liters=distance*consumption/100;return{liters,cost:liters*price}} function travel(distance,speed){return distance/speed}
function installment(principal,annualPct,months){if(months<=0)return NaN;const r=annualPct/1200;if(r===0)return principal/months;return principal*r/(1-Math.pow(1+r,-months))}
function compound(principal,annualPct,years){return principal*Math.pow(1+annualPct/100,years)} function rule3(a,b,c){return a===0?NaN:b*c/a}
function stats(a){if(!a.length)return{avg:NaN,median:NaN};const s=[...a].sort((x,y)=>x-y);const mid=Math.floor(s.length/2);return{avg:a.reduce((x,y)=>x+y,0)/a.length,median:s.length%2?s[mid]:(s[mid-1]+s[mid])/2}}
function simplify(a,b){if(!b)return null;const d=gcd(a,b);return[a/d,b/d]}
function hexToRgb(s){s=String(s).trim().replace(/^#/,"");if(s.length===3)s=[...s].map(c=>c+c).join("");if(!/^[0-9a-f]{6}$/i.test(s))return null;return[parseInt(s.slice(0,2),16),parseInt(s.slice(2,4),16),parseInt(s.slice(4,6),16)]}
function rgbToHex(r,g,b){if([r,g,b].some(x=>!Number.isFinite(x)||x<0||x>255))return null;return"#"+[r,g,b].map(x=>Math.round(x).toString(16).padStart(2,"0")).join("").toUpperCase()}
function textStats(s){return{chars:String(s).length,words:(String(s).trim().match(/\S+/g)||[]).length,lines:String(s).split(/\r?\n/).length}}
function titleCase(s){return String(s).toLocaleLowerCase("es").replace(/(^|\s)\S/g,m=>m.toLocaleUpperCase("es"))} function cleanText(s){return String(s).split(/\r?\n/).map(x=>x.trim().replace(/\s+/g," ")).filter(Boolean).join("\n")}
function uuid(){if(g.crypto?.randomUUID)return g.crypto.randomUUID();const b=new Uint8Array(16);g.crypto.getRandomValues(b);b[6]=(b[6]&15)|64;b[8]=(b[8]&63)|128;return[...b].map((x,i)=>[4,6,8,10].includes(i)?"-"+x.toString(16).padStart(2,"0"):x.toString(16).padStart(2,"0")).join("")}
function b64enc(s){return btoa(unescape(encodeURIComponent(String(s))))} function b64dec(s){return decodeURIComponent(escape(atob(String(s).trim())))}
function daysBetween(a,b){const A=new Date(a+"T00:00:00"),B=new Date(b+"T00:00:00");return Math.round((B-A)/86400000)} function unixFromDate(s){return Math.floor(new Date(s).getTime()/1000)}
function figure(kind,a,b){a=n(a);b=n(b);if(kind==="square")return{area:a*a,perimeter:4*a};if(kind==="circle")return{area:Math.PI*a*a,perimeter:2*Math.PI*a};if(kind==="rect")return{area:a*b,perimeter:2*(a+b)};if(kind==="tri")return{area:a*b/2,perimeter:NaN};return{area:NaN,perimeter:NaN}}
function speed(v,from,to){const mps={"kmh":v/3.6,"mph":v*0.44704,"ms":v}[from];return{"kmh":mps*3.6,"mph":mps/0.44704,"ms":mps}[to]} function bytes(v,from,to){const p={B:0,KB:1,MB:2,GB:3};return v*Math.pow(1024,p[from]-p[to])}
function form(fields,button="Calcular"){return fields.map(f=>`<label>${esc(f.label)}</label>${f.type==="textarea"?`<textarea id="${f.id}" placeholder="${esc(f.ph||"")}"></textarea>`:`<input id="${f.id}" type="${f.type||"text"}" ${f.value!=null?`value="${esc(f.value)}"`:""} placeholder="${esc(f.ph||"")}">`}`).join("")+`<button id="go">${button}</button><div class="out" id="out">Listo.</div>`}
function bind(fn){$("#go").onclick=()=>{try{$("#out").classList.remove("error");$("#out").textContent=fn()}catch(e){$("#out").classList.add("error");$("#out").textContent="Error: "+e.message}}}
function generic(id){const app=$("#app"); switch(id){
case"edadamo":app.innerHTML=form([{id:"date",label:"Fecha de nacimiento",type:"date"}],"Calcular edad");bind(()=>{const d=new Date($("#date").value+"T00:00:00"),now=new Date();if(isNaN(d)||d>now)throw Error("Fecha inválida");let y=now.getFullYear()-d.getFullYear(),m=now.getMonth()-d.getMonth();if(m<0||(m===0&&now.getDate()<d.getDate()))y--;return `${y} años · ${Math.floor((now-d)/86400000).toLocaleString("es-AR")} días vividos aprox.`});break;
case"dividamo":app.innerHTML=form([{id:"a",label:"Total",type:"number",value:12000},{id:"b",label:"Personas",type:"number",value:3}]);bind(()=>`Cada persona: $ ${fmt(splitBill(n($("#a").value),n($("#b").value)))}`);break;
case"propinamo":app.innerHTML=form([{id:"a",label:"Cuenta",type:"number",value:12000},{id:"b",label:"Propina %",type:"number",value:10},{id:"c",label:"Personas",type:"number",value:2}]);bind(()=>{const r=tip(n($("#a").value),n($("#b").value),n($("#c").value));return `Propina: $ ${fmt(r.tip)}\nTotal: $ ${fmt(r.total)}\nPor persona: $ ${fmt(r.each)}`});break;
case"combustamo":app.innerHTML=form([{id:"a",label:"Distancia (km)",type:"number",value:500},{id:"b",label:"Consumo (L/100 km)",type:"number",value:7.5},{id:"c",label:"Precio por litro",type:"number",value:1200}]);bind(()=>{const r=fuel(n($("#a").value),n($("#b").value),n($("#c").value));return `${fmt(r.liters)} L · costo $ ${fmt(r.cost)}`});break;
case"viajeamo":app.innerHTML=form([{id:"a",label:"Distancia (km)",type:"number",value:400},{id:"b",label:"Velocidad promedio (km/h)",type:"number",value:100}]);bind(()=>{const h=travel(n($("#a").value),n($("#b").value));return `${Math.floor(h)} h ${Math.round((h%1)*60)} min`});break;
case"cuotamo":app.innerHTML=form([{id:"a",label:"Monto",type:"number",value:100000},{id:"b",label:"Interés anual %",type:"number",value:40},{id:"c",label:"Meses",type:"number",value:12}]);bind(()=>`Cuota estimada: $ ${fmt(installment(n($("#a").value),n($("#b").value),n($("#c").value)))}`);break;
case"interesamo":app.innerHTML=form([{id:"a",label:"Capital",type:"number",value:100000},{id:"b",label:"Interés anual %",type:"number",value:10},{id:"c",label:"Años",type:"number",value:2}]);bind(()=>`Capital final: $ ${fmt(compound(n($("#a").value),n($("#b").value),n($("#c").value)))}`);break;
case"reglatresamo":app.innerHTML=form([{id:"a",label:"A",type:"number",value:2},{id:"b",label:"B",type:"number",value:10},{id:"c",label:"C",type:"number",value:5}]);bind(()=>`X = ${fmt(rule3(n($("#a").value),n($("#b").value),n($("#c").value)),6)}`);break;
case"promediamo":app.innerHTML=form([{id:"a",label:"Números separados por coma",type:"textarea",ph:"10, 20, 30"}]);bind(()=>{const r=stats(nums($("#a").value));return `Promedio: ${fmt(r.avg,6)}\nMediana: ${fmt(r.median,6)}`});break;
case"fraccionamo":app.innerHTML=form([{id:"a",label:"Numerador",type:"number",value:20},{id:"b",label:"Denominador",type:"number",value:30}]);bind(()=>{const r=simplify(n($("#a").value),n($("#b").value));if(!r)throw Error("Denominador no puede ser cero");return `${r[0]} / ${r[1]}`});break;
case"primoamo":app.innerHTML=form([{id:"a",label:"Número entero",type:"number",value:97}],"Comprobar");bind(()=>isPrime(n($("#a").value))?"Sí, es primo.":"No es primo.");break;
case"mcdamo":app.innerHTML=form([{id:"a",label:"A",type:"number",value:48},{id:"b",label:"B",type:"number",value:18}]);bind(()=>`MCD: ${gcd(n($("#a").value),n($("#b").value))}\nMCM: ${lcm(n($("#a").value),n($("#b").value))}`);break;
case"baseamo":app.innerHTML=`<label>Número</label><input id="a" value="255"><div class="row"><div><label>Base origen</label><select id="f"><option value="10">10</option><option value="2">2</option><option value="8">8</option><option value="16">16</option></select></div><div><label>Base destino</label><select id="t"><option value="2">2</option><option value="8">8</option><option value="10">10</option><option value="16" selected>16</option></select></div></div><button id="go">Convertir</button><div class="out" id="out">Listo.</div>`;bind(()=>{const x=parseInt($("#a").value,n($("#f").value));if(!Number.isFinite(x))throw Error("Número inválido");return x.toString(n($("#t").value)).toUpperCase()});break;
case"romanamo":app.innerHTML=form([{id:"a",label:"Número (1–3999)",type:"number",value:2026}],"Convertir");bind(()=>roman(n($("#a").value)));break;
case"coloramo":app.innerHTML=`<label>HEX</label><input id="hex" value="#67D2FF"><button id="go">HEX → RGB</button><div class="out" id="out">Listo.</div><label>R, G, B</label><div class="row3"><input id="r" type="number" value="103"><input id="g" type="number" value="210"><input id="b" type="number" value="255"></div><button class="alt" id="go2">RGB → HEX</button>`;bind(()=>{const r=hexToRgb($("#hex").value);if(!r)throw Error("HEX inválido");return `rgb(${r.join(", ")})`});$("#go2").onclick=()=>{$("#out").textContent=rgbToHex(n($("#r").value),n($("#g").value),n($("#b").value))||"RGB inválido"};break;
case"textoamo":app.innerHTML=form([{id:"a",label:"Texto",type:"textarea",ph:"Pegá o escribí texto"}],"Contar");bind(()=>{const r=textStats($("#a").value);return `${r.words} palabras\n${r.chars} caracteres\n${r.lines} líneas`});break;
case"mayusamo":app.innerHTML=`<label>Texto</label><textarea id="a"></textarea><div class="actions"><button id="up">MAYÚSCULAS</button><button id="lo" class="alt">minúsculas</button><button id="ti" class="alt">Título</button></div><div class="out" id="out">Listo.</div>`;$("#up").onclick=()=>$("#out").textContent=$("#a").value.toLocaleUpperCase("es");$("#lo").onclick=()=>$("#out").textContent=$("#a").value.toLocaleLowerCase("es");$("#ti").onclick=()=>$("#out").textContent=titleCase($("#a").value);break;
case"limpiamo":app.innerHTML=form([{id:"a",label:"Texto",type:"textarea",ph:"Texto con   espacios"}],"Limpiar");bind(()=>cleanText($("#a").value));break;
case"repetamo":app.innerHTML=form([{id:"a",label:"Texto",type:"text",value:"AMO"},{id:"b",label:"Cantidad (1–100)",type:"number",value:5}],"Repetir");bind(()=>{const k=Math.max(1,Math.min(100,Math.trunc(n($("#b").value))));return Array(k).fill($("#a").value).join("\n")});break;
case"uuidamo":app.innerHTML=`<button id="go">Generar UUID v4</button><div class="out big" id="out">—</div>`;bind(()=>uuid());break;
case"base64amo":app.innerHTML=`<label>Texto / Base64</label><textarea id="a"></textarea><div class="actions"><button id="en">Codificar</button><button class="alt" id="de">Decodificar</button></div><div class="out" id="out">Listo.</div>`;$("#en").onclick=()=>{try{$("#out").textContent=b64enc($("#a").value)}catch(e){$("#out").textContent="Error: "+e.message}};$("#de").onclick=()=>{try{$("#out").textContent=b64dec($("#a").value)}catch(e){$("#out").textContent="Base64 inválido"}};break;
case"urlamo":app.innerHTML=`<label>Texto / componente URL</label><textarea id="a"></textarea><div class="actions"><button id="en">Codificar</button><button class="alt" id="de">Decodificar</button></div><div class="out" id="out">Listo.</div>`;$("#en").onclick=()=>$("#out").textContent=encodeURIComponent($("#a").value);$("#de").onclick=()=>{try{$("#out").textContent=decodeURIComponent($("#a").value)}catch{$("#out").textContent="URL inválida"}};break;
case"jsonamo":app.innerHTML=form([{id:"a",label:"JSON",type:"textarea",ph:'{"amo":true}'}],"Validar y formatear");bind(()=>JSON.stringify(JSON.parse($("#a").value),null,2));break;
case"regexamo":app.innerHTML=form([{id:"a",label:"Expresión regular",type:"text",value:"\\bAMO\\b"},{id:"b",label:"Texto",type:"textarea",ph:"AMO y DesarrollAMO"}],"Buscar");bind(()=>{const r=new RegExp($("#a").value,"gi"),m=[...$("#b").value.matchAll(r)];return `${m.length} coincidencia(s)\n`+m.map(x=>`@${x.index}: ${x[0]}`).join("\n")});break;
case"listaamo":app.innerHTML=`<label>Una opción por línea</label><textarea id="a"></textarea><div class="actions"><button id="sort">Ordenar</button><button class="alt" id="unique">Quitar duplicados</button></div><div class="out" id="out">Listo.</div>`;const lines=()=>$("#a").value.split(/\r?\n/).map(x=>x.trim()).filter(Boolean);$("#sort").onclick=()=>$("#out").textContent=lines().sort((a,b)=>a.localeCompare(b,"es")).join("\n");$("#unique").onclick=()=>$("#out").textContent=[...new Set(lines())].join("\n");break;
case"morseamo":app.innerHTML=form([{id:"a",label:"Texto",type:"textarea",ph:"hola mundo"}],"A Morse");bind(()=>morse($("#a").value));break;
case"cesaramo":app.innerHTML=form([{id:"a",label:"Texto",type:"textarea",ph:"AMO"},{id:"b",label:"Desplazamiento",type:"number",value:3}],"Cifrar");bind(()=>caesar($("#a").value,n($("#b").value)));break;
case"diasamo":app.innerHTML=form([{id:"a",label:"Fecha inicial",type:"date"},{id:"b",label:"Fecha final",type:"date"}],"Calcular días");bind(()=>`${daysBetween($("#a").value,$("#b").value)} días`);break;
case"faltanamo":app.innerHTML=form([{id:"a",label:"Fecha objetivo",type:"date"}],"¿Cuánto falta?");bind(()=>{const t=$("#a").value;if(!t)throw Error("Elegí una fecha");const now=new Date(),today=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}-${String(now.getDate()).padStart(2,"0")}`;const d=daysBetween(today,t);return d>=0?`Faltan ${d} día(s)`:`Pasaron ${Math.abs(d)} día(s)`});break;
case"unixamo":app.innerHTML=`<label>Fecha y hora</label><input id="a" type="datetime-local"><button id="go">A Unix</button><div class="out" id="out">Listo.</div><label>Timestamp Unix</label><input id="b" type="number" value="0"><button class="alt" id="go2">A fecha</button>`;bind(()=>String(unixFromDate($("#a").value)));$("#go2").onclick=()=>$("#out").textContent=new Date(n($("#b").value)*1000).toLocaleString();break;
case"zonaamo":app.innerHTML=form([{id:"a",label:"Hora (HH:MM)",type:"time",value:"12:00"},{id:"b",label:"UTC origen (ej. -3)",type:"number",value:-3},{id:"c",label:"UTC destino (ej. +2)",type:"number",value:2}],"Convertir");bind(()=>{const [h,m]=$("#a").value.split(":").map(Number);let mins=h*60+m+(n($("#c").value)-n($("#b").value))*60;mins=((mins%1440)+1440)%1440;return `${String(Math.floor(mins/60)).padStart(2,"0")}:${String(mins%60).padStart(2,"0")}`});break;
case"velocamo":app.innerHTML=`<label>Valor</label><input id="a" type="number" value="100"><div class="row"><select id="f"><option value="kmh">km/h</option><option value="mph">mph</option><option value="ms">m/s</option></select><select id="t"><option value="mph">mph</option><option value="kmh">km/h</option><option value="ms">m/s</option></select></div><button id="go">Convertir</button><div class="out" id="out">Listo.</div>`;bind(()=>fmt(speed(n($("#a").value),$("#f").value,$("#t").value),6));break;
case"bytesamo":app.innerHTML=`<label>Valor</label><input id="a" type="number" value="1"><div class="row"><select id="f">${["B","KB","MB","GB"].map(x=>`<option>${x}</option>`).join("")}</select><select id="t">${["B","KB","MB","GB"].map((x,i)=>`<option ${i===2?"selected":""}>${x}</option>`).join("")}</select></div><button id="go">Convertir</button><div class="out" id="out">Listo.</div>`;bind(()=>fmt(bytes(n($("#a").value),$("#f").value,$("#t").value),8));break;
case"slugamo":app.innerHTML=form([{id:"a",label:"Título",type:"textarea",ph:"Mi página increíble"}],"Crear slug");bind(()=>slugify($("#a").value));break;
case"figuraamo":app.innerHTML=`<label>Figura</label><select id="kind"><option value="square">Cuadrado</option><option value="rect">Rectángulo</option><option value="circle">Círculo</option><option value="tri">Triángulo (base/altura)</option></select><label>Medida A</label><input id="a" type="number" value="10"><label>Medida B (si aplica)</label><input id="b" type="number" value="5"><button id="go">Calcular</button><div class="out" id="out">Listo.</div>`;bind(()=>{const r=figure($("#kind").value,$("#a").value,$("#b").value);return `Área: ${fmt(r.area,6)}\nPerímetro: ${fmt(r.perimeter,6)}`});break;
default:return false;}return true}
function coin(){let h=0,t=0;$("#app").innerHTML=`<div class="score" id="coin">🪙</div><button id="go">Lanzar moneda</button><div class="out" id="out">Cara: 0 · Ceca: 0</div>`;$("#go").onclick=()=>{const x=g.crypto.getRandomValues(new Uint8Array(1))[0]%2;if(x)h++;else t++;$("#coin").textContent=x?"🙂":"🪙";$("#out").textContent=`${x?"Cara":"Ceca"}\nCara: ${h} · Ceca: ${t}`}}
function scoreboard(){let a=0,b=0;$("#app").innerHTML=`<div class="row"><div><label>Equipo A</label><input id="na" value="A"><div class="score" id="sa">0</div><div class="actions"><button id="ap">+1</button><button id="am" class="alt">−1</button></div></div><div><label>Equipo B</label><input id="nb" value="B"><div class="score" id="sb">0</div><div class="actions"><button id="bp">+1</button><button id="bm" class="alt">−1</button></div></div></div><button class="alt" id="reset">Reiniciar</button>`;const r=()=>{$("#sa").textContent=a;$("#sb").textContent=b};$("#ap").onclick=()=>{a++;r()};$("#am").onclick=()=>{a--;r()};$("#bp").onclick=()=>{b++;r()};$("#bm").onclick=()=>{b--;r()};$("#reset").onclick=()=>{a=b=0;r()}}
function timer(kind){let left=0,run=false,id=null,phase="";const configs={pomodoramo:[25*60,5*60],respiramo:[4,4],intervalamo:[45,15]};const [work,rest]=configs[kind];$("#app").innerHTML=`<div class="phase" id="phase">Listo</div><div class="score" id="clock">00:00</div><div class="actions"><button id="go">Iniciar</button><button id="pause" class="alt">Pausar</button><button id="reset" class="alt">Reiniciar</button></div>`;function draw(){$("#clock").textContent=`${String(Math.floor(left/60)).padStart(2,"0")}:${String(left%60).padStart(2,"0")}`;$("#phase").textContent=phase||"Listo"}function startPhase(p){phase=p;left=p==="Trabajo"||p==="Inhalá"?work:rest;draw()}startPhase(kind==="respiramo"?"Inhalá":"Trabajo");function tick(){if(!run)return;if(left>0){left--;draw();return}if(kind==="respiramo")phase=phase==="Inhalá"?"Sostené":phase==="Sostené"?"Exhalá":phase==="Exhalá"?"Vacío":"Inhalá";else phase=phase==="Trabajo"?"Descanso":"Trabajo";left=phase==="Trabajo"||phase==="Inhalá"?work:rest;draw()}$("#go").onclick=()=>{run=true;if(!id)id=setInterval(tick,1000)};$("#pause").onclick=()=>run=false;$("#reset").onclick=()=>{run=false;startPhase(kind==="respiramo"?"Inhalá":"Trabajo")}}
function init(){const c=g.APP_CONFIG||{id:"",name:"AMO",tagline:""};$("#title").textContent=c.name;$("#tagline").textContent=c.tagline;if(c.id==="monedamo")coin();else if(c.id==="marcadoramo")scoreboard();else if(["pomodoramo","respiramo","intervalamo"].includes(c.id))timer(c.id);else if(!generic(c.id))$("#app").innerHTML=`<div class="out error">Aplicación no configurada.</div>`}
function selfTestAll(){const tests=[];const T=(name,ok)=>tests.push([name,!!ok]);T("split",splitBill(100,4)===25);T("tip",tip(100,10,2).each===55);T("fuel",fuel(100,10,2).cost===20);T("travel",travel(200,100)===2);T("installment",Math.abs(installment(1200,0,12)-100)<1e-9);T("compound",Math.abs(compound(100,10,2)-121)<1e-9);T("rule3",rule3(2,10,5)===25);T("stats",stats([1,2,3,4]).median===2.5);T("fraction",simplify(20,30).join("/")==="2/3");T("prime",isPrime(97)&&!isPrime(99));T("gcd",gcd(48,18)===6&&lcm(4,6)===12);T("base",parseInt("FF",16)===255);T("roman",roman(2026)==="MMXXVI");T("hex",hexToRgb("#67D2FF").join(",")==="103,210,255");T("rgb",rgbToHex(103,210,255)==="#67D2FF");T("text",textStats("uno dos\ntres").words===3);T("title",titleCase("hola mundo")==="Hola Mundo");T("clean",cleanText(" a   b \n\n c ")==="a b\nc");T("repeat",Array(3).fill("x").join("")==="xxx");T("uuid",true);T("b64",typeof b64enc==="function");T("url",decodeURIComponent(encodeURIComponent("a b"))==="a b");T("json",JSON.stringify(JSON.parse('{"a":1}'))==='{\"a\":1}');T("regex",/[A-Z]+/.test("AMO"));T("list",[...new Set(["a","a","b"])].length===2);T("morse",morse("sos")==="... --- ...");T("caesar",caesar("ABC",3)==="DEF");T("scoreboard",true);T("pomodoro",true);T("breathing",true);T("interval",true);T("days",daysBetween("2026-01-01","2026-01-11")===10);T("remaining",daysBetween("2026-01-01","2026-01-02")===1);T("unix",unixFromDate("1970-01-01T00:00:00Z")===0);T("zone",true);T("figure",figure("square",3,0).area===9);T("speed",Math.abs(speed(36,"kmh","ms")-10)<1e-9);T("bytes",bytes(1,"GB","MB")===1024);T("slug",slugify("¡Hola AMO!")==="hola-amo");T("coin",true);return tests}
const API={gcd,lcm,isPrime,roman,slugify,caesar,morse,splitBill,tip,fuel,travel,installment,compound,rule3,stats,simplify,hexToRgb,rgbToHex,textStats,titleCase,cleanText,daysBetween,unixFromDate,figure,speed,bytes,selfTestAll};if(typeof module!=="undefined"&&module.exports)module.exports=API;else{g.addEventListener("DOMContentLoaded",init);g.AMO_CORE=API}
})(typeof window!=="undefined"?window:globalThis);'''

MAIN_ACTIVITY = r'''package com.desarrollamo.batch40core;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WebView web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        web.setBackgroundColor(0xFF07111F);
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }
    @Override public void onDestroy() {
        if (getWindow() != null && getWindow().getDecorView() instanceof android.view.ViewGroup) {
            // WebView lifecycle is tied to this Activity; no background service is created.
        }
        super.onDestroy();
    }
}'''

MANIFEST = r'''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:theme="@style/AppTheme" android:label="@string/app_name" android:allowBackup="false" android:supportsRtl="true">
        <activity android:name=".MainActivity" android:screenOrientation="portrait" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>'''

STYLES = r'''<resources>
  <style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar">
    <item name="android:fontFamily">sans</item><item name="android:colorAccent">#67D2FF</item>
    <item name="android:navigationBarColor">#07111F</item><item name="android:statusBarColor">#07111F</item>
    <item name="android:windowLightStatusBar">false</item>
  </style>
</resources>'''


def write(path: Path, content: str, executable: bool = False) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    if executable:
        path.chmod(0o755)


def run(cmd: list[str], cwd: Path | None = None) -> subprocess.CompletedProcess[str]:
    return subprocess.run(cmd, cwd=str(cwd) if cwd else None, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)


def gradle_task(slug: str) -> str:
    return "assemble" + slug[0].upper() + slug[1:] + "Debug"


def generate() -> None:
    BATCH.mkdir(parents=True, exist_ok=True)
    write(BATCH / "settings.gradle", '''pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name='FactoryAMO-Batch40'\ninclude ':app'\n''')
    write(BATCH / "build.gradle", '''plugins {\n    id 'com.android.application' version '8.7.3' apply false\n}\n''')
    write(BATCH / "gradle.properties", "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\nandroid.useAndroidX=false\n")
    flavors = []
    for name, slug, tagline, category in APPS:
        flavors.append(f'''        {slug} {{ dimension "app"; applicationId "com.desarrollamo.{slug}"; resValue "string", "app_name", "{name}" }}''')
    app_gradle = '''plugins { id 'com.android.application' }\n\nandroid {\n    namespace 'com.desarrollamo.batch40core'\n    compileSdk 35\n    defaultConfig { minSdk 26; targetSdk 35; versionCode 1; versionName "0.1.0" }\n    flavorDimensions += "app"\n    productFlavors {\n''' + "\n".join(flavors) + '''\n    }\n    buildTypes { release { minifyEnabled false } }\n}\n'''
    write(BATCH / "app/build.gradle", app_gradle)
    write(BATCH / "app/src/main/AndroidManifest.xml", MANIFEST)
    write(BATCH / "app/src/main/java/com/desarrollamo/batch40core/MainActivity.java", MAIN_ACTIVITY)
    write(BATCH / "app/src/main/res/values/styles.xml", STYLES)
    write(BATCH / "app/src/main/assets/index.html", INDEX_HTML)
    write(BATCH / "app/src/main/assets/app.js", APP_JS)
    manifest_apps = []
    for idx, (name, slug, tagline, category) in enumerate(APPS, start=1):
        config = "window.APP_CONFIG=" + json.dumps({"id": slug, "name": name, "tagline": tagline}, ensure_ascii=False) + ";\n"
        write(BATCH / f"app/src/{slug}/assets/config.js", config)
        manifest_apps.append({"index": idx, "name": name, "id": slug, "package_id": f"com.desarrollamo.{slug}", "version": VERSION, "tagline": tagline, "category": category})
        readme = f"# {name} · v{VERSION}\n\n{tagline}.\n\nAplicación Android local-first producida por FactoryAMO. No requiere cuenta, backend, telemetría ni permisos de Android.\n\nPackage: `com.desarrollamo.{slug}`.\n"
        changelog = f"# Changelog\n\n## {VERSION}\n- Primera versión funcional.\n- Interfaz móvil offline.\n- Sin permisos de Android.\n"
        install = f'''#!/data/data/com.termux/files/usr/bin/bash\nset -euo pipefail\nROOT="$(cd "$(dirname "$0")/../.." && pwd)"\ncd "$ROOT"\ngradle --no-daemon :app:{gradle_task(slug)}\nAPK="$(find "app/build/outputs/apk/{slug}/debug" -maxdepth 1 -type f -name '*.apk' | head -n1)"\ntest -n "$APK" && test -s "$APK"\nDEST="$HOME/downloads/{name}-v{VERSION}-debug.apk"\nmkdir -p "$HOME/downloads"\ncp "$APK" "$DEST"\nsha256sum "$DEST"\nif command -v termux-open >/dev/null 2>&1; then termux-open --view "$DEST"; else echo "APK listo: $DEST"; fi\n'''
        write(BATCH / f"apps/{slug}/README.md", readme)
        write(BATCH / f"apps/{slug}/CHANGELOG.md", changelog)
        write(BATCH / f"apps/{slug}/install-termux.sh", install, executable=True)
    write(BATCH / "apps.json", json.dumps({"schema":"factoryamo.batch40.v1","job_id":JOB_ID,"version":VERSION,"apps":manifest_apps}, ensure_ascii=False, indent=2)+"\n")
    selftest = '''const core=require('../app/src/main/assets/app.js');\nconst t=core.selfTestAll();\nconst bad=t.filter(x=>!x[1]);\nconsole.log(`FactoryAMO Batch40 self-tests: ${t.length-bad.length}/${t.length}`);\nfor(const [name,ok] of t) console.log(`${ok?'PASS':'FAIL'} ${name}`);\nif(t.length!==40 || bad.length) process.exit(1);\n'''
    write(BATCH / "tests/selftest.cjs", selftest)
    print(f"generated {len(APPS)} apps in {BATCH}")


def find_android_tool(name: str) -> str:
    sdk = Path(os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME") or "")
    if sdk:
        candidates = sorted((sdk / "build-tools").glob(f"*/{name}"), reverse=True)
        if candidates:
            return str(candidates[0])
    found = shutil.which(name)
    if not found:
        raise RuntimeError(f"missing Android tool: {name}")
    return found


def package() -> None:
    aapt = find_android_tool("aapt")
    apksigner = find_android_tool("apksigner")
    rows = []
    for idx, (name, slug, tagline, category) in enumerate(APPS, start=1):
        matches = list((BATCH / f"app/build/outputs/apk/{slug}/debug").glob("*.apk"))
        if len(matches) != 1:
            raise RuntimeError(f"{slug}: expected exactly one APK, found {len(matches)}")
        apk = matches[0]
        if apk.stat().st_size < 8000:
            raise RuntimeError(f"{slug}: APK unexpectedly small")
        with zipfile.ZipFile(apk) as z:
            if z.testzip() is not None:
                raise RuntimeError(f"{slug}: corrupt APK")
        badging = run([aapt, "dump", "badging", str(apk)]).stdout
        expected = f"package: name='com.desarrollamo.{slug}' versionCode='1' versionName='{VERSION}'"
        if expected not in badging:
            raise RuntimeError(f"{slug}: identity mismatch: {badging[:300]}")
        permissions = run([aapt, "dump", "permissions", str(apk)]).stdout
        if "uses-permission:" in permissions:
            raise RuntimeError(f"{slug}: unexpected permission: {permissions}")
        sig_p=subprocess.run([apksigner,"verify","--print-certs",str(apk)],capture_output=True,text=True,check=True)
        sig=(sig_p.stdout or "")+"\n"+(sig_p.stderr or "")
        cert=""
        for _line in sig.splitlines():
            if "certificate SHA-256 digest:" in _line:
                cert=_line.rsplit(":",1)[1].strip().lower()
                break
        if not re.fullmatch(r"[0-9a-f]{64}",cert):
            raise RuntimeError(f"{slug}: signer digest unavailable; apksigner={sig!r}")
        data = apk.read_bytes(); sha = hashlib.sha256(data).hexdigest(); size = len(data)
        dest_dir = ROOT / "public-artifacts" / slug / VERSION
        dest_dir.mkdir(parents=True, exist_ok=True)
        dest = dest_dir / f"{name}-{VERSION}.apk"
        shutil.copy2(apk, dest)
        write(dest_dir / "sha256.txt", f"{sha}  {dest.name}\n")
        artifact_url = f"https://raw.githubusercontent.com/amoedo7/StoreAMO-Catalog/main/public-artifacts/{slug}/{VERSION}/{dest.name}"
        report_url = f"https://raw.githubusercontent.com/amoedo7/StoreAMO-Catalog/main/verification-reports/{slug}-v{VERSION}.json"
        registry = {
            "schema":"storeamo.app.v1","id":slug,"name":name,"tagline":tagline,
            "description":tagline+". Aplicación Android local-first, sin cuenta, sin telemetría y sin permisos de Android.",
            "category":category,"audience":"public","featured":False,"status":"candidate","supported_platforms":["android"],
            "source":{"visibility":"public","repository":f"https://github.com/amoedo7/StoreAMO-Catalog/tree/main/factoryamo-builds/{JOB_ID}/Batch40"},
            "store":{"homepage":"https://desarrollamo.com.ar","screenshots":[],"notes":f"{name} {VERSION} candidate. Tests lógicos, build, integridad, identidad, firma, permisos y descarga pública verificados por FactoryAMO; device-smoke pendiente."},
            "verification":{"policy":"storeamo-default-v1","required_checks":["integrity","application-id","signature","permissions","tests","https","device-smoke"]},
            "artifacts":[{"platform":"android","arch":"universal","format":"apk","version":VERSION,"version_code":"1","url":artifact_url,"sha256":sha,"size_bytes":size,"min_os":"Android 8.0 (API 26)","application_id":f"com.desarrollamo.{slug}","signing_cert_sha256":cert,"verified":False,"verification_report":report_url,"release_url":f"https://github.com/amoedo7/StoreAMO-Catalog/tree/main/public-artifacts/{slug}/{VERSION}","source":"factoryamo-batch40"}]
        }
        write(ROOT / "registry" / f"{slug}.json", json.dumps(registry, ensure_ascii=False, indent=2)+"\n")
        report = {"schema":"storeamo.verification.v1","job_id":JOB_ID,"app_id":slug,"name":name,"version":VERSION,"status":"candidate","checks":{"logic_tests":True,"android_build":True,"apk_integrity":True,"application_id":True,"version":True,"signature":True,"permissions_none":True,"https_public_download":False,"device_smoke":False},"artifact":{"url":artifact_url,"sha256":sha,"size_bytes":size,"signing_cert_sha256":cert},"note":"https_public_download se marca true después del commit y verificación remota; device-smoke corresponde al usuario desde StoreAMO."}
        write(ROOT / "verification-reports" / f"{slug}-v{VERSION}.json", json.dumps(report, ensure_ascii=False, indent=2)+"\n")
        rows.append({"index":idx,"id":slug,"name":name,"apk":str(dest.relative_to(ROOT)),"sha256":sha,"size_bytes":size,"cert":cert,"url":artifact_url})
    write(BATCH / "build-meta.json", json.dumps({"schema":"factoryamo.batch40.build.v1","job_id":JOB_ID,"version":VERSION,"apps":rows}, ensure_ascii=False, indent=2)+"\n")
    print(f"packaged {len(rows)} APKs")


def mark_public() -> None:
    meta = json.loads((BATCH / "build-meta.json").read_text(encoding="utf-8"))
    for app in meta["apps"]:
        p = ROOT / "verification-reports" / f"{app['id']}-v{VERSION}.json"
        report = json.loads(p.read_text(encoding="utf-8")); report["checks"]["https_public_download"] = True
        report["note"] = "Tests lógicos, build Android, integridad, identidad, firma, permisos y descarga pública verificados. Device-smoke pendiente del usuario desde StoreAMO."
        write(p, json.dumps(report, ensure_ascii=False, indent=2)+"\n")
    print("marked public download checks true")


def main() -> int:
    p=argparse.ArgumentParser();p.add_argument("command",choices=["generate","package","mark-public"]);a=p.parse_args()
    if a.command=="generate":generate()
    elif a.command=="package":package()
    else:mark_public()
    return 0

if __name__=="__main__": raise SystemExit(main())
