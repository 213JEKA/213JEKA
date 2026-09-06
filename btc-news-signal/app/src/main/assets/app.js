let events=[];let market=null;let timer=null;
const $=id=>document.getElementById(id);
const fmtPct=n=>`${n>=0?'+':''}${n.toFixed(3)}%`;
const pad=n=>String(n).padStart(2,'0');

window.receiveEvents=json=>{
  try{events=JSON.parse(json).filter(e=>e.timeMillis>Date.now()-3600000);renderEvents();renderNext();}
  catch(e){showToast('Ошибка календаря');}
};
window.receiveMarket=json=>{try{market=JSON.parse(json);renderMarket();renderSignal();}catch(e){showToast('Ошибка данных BTC');}};
window.receiveError=showToast;

function nextEvent(){return events.find(e=>e.timeMillis>Date.now()-60000)||null}
function renderNext(){
  const e=nextEvent();if(!e){$('eventType').textContent='—';$('eventTitle').textContent='Нет запланированных релизов';$('eventDate').textContent='Обновите календарь';return}
  $('eventType').textContent=e.type;$('eventTitle').textContent=e.title;$('quality').textContent=`историческое совпадение ${e.quality}%`;
  $('eventDate').textContent=new Date(e.timeMillis).toLocaleString('ru-RU',{weekday:'long',day:'numeric',month:'long',hour:'2-digit',minute:'2-digit'});
  tick();
}
function tick(){
  const e=nextEvent();if(!e)return;const d=e.timeMillis-Date.now();
  if(d<=-60000){renderNext();return}
  const total=Math.max(0,Math.floor(d/1000)),days=Math.floor(total/86400),hours=Math.floor(total%86400/3600),mins=Math.floor(total%3600/60),secs=total%60;
  $('countdown').textContent=days?`${days}д ${pad(hours)}:${pad(mins)}:${pad(secs)}`:`${pad(hours)}:${pad(mins)}:${pad(secs)}`;
  $('phase').textContent=d>3600000?'Анализ начнётся за 60 минут':d>300000?'Идёт предварительный анализ BTC':d>60000?'Финальное окно — проверьте сигнал':'До публикации меньше минуты';
  renderSignal();
}
function renderMarket(){
  $('price').textContent=`$${market.price.toLocaleString('en-US',{maximumFractionDigits:2})}`;$('p30').textContent=fmtPct(market.p30);$('p60').textContent=fmtPct(market.p60);
  setDirection($('p30'),market.p30);setDirection($('p60'),market.p60);$('alignment').textContent=market.aligned?'30м и 60м совпадают':'направления не совпадают';
  $('updated').textContent=`обновлено ${new Date(market.updatedAt).toLocaleTimeString('ru-RU',{hour:'2-digit',minute:'2-digit',second:'2-digit'})}`;
}
function setDirection(el,n){el.classList.remove('positive','negative');el.classList.add(n>0?'positive':n<0?'negative':'')}
function renderSignal(){
  const card=$('signalCard'),e=nextEvent();card.className='signal wait';let text='НЕТ СИГНАЛА',hint='Ждём окно за 60 минут до релиза';
  if(!e){hint='Нет ближайшей новости'}else if(e.timeMillis-Date.now()<=3600000&&market){
    if(market.signal==='BUY'){card.className='signal buy';text='BUY EUR/USD';hint=`BTC ${fmtPct(market.p30)} за 30м · ${e.type}: удержание ${e.hold}`}
    else if(market.signal==='SELL'){card.className='signal sell';text='SELL EUR/USD';hint=`BTC ${fmtPct(market.p30)} за 30м · ${e.type}: удержание ${e.hold}`}
    else{hint=Math.abs(market.p30)<market.threshold?'Импульс BTC слабее 0,15%':'30м и 60м не подтверждают друг друга'}
  }
  $('signalText').textContent=text;$('signalHint').textContent=hint;
}
function renderEvents(){
  $('events').innerHTML=events.slice(0,8).map(e=>{const d=new Date(e.timeMillis);return `<div class="event-item"><span class="event-kind">${e.type}</span><div><b>${d.toLocaleDateString('ru-RU',{day:'numeric',month:'short'})}</b><small>${e.hold}</small></div><div class="event-time">${d.toLocaleTimeString('ru-RU',{hour:'2-digit',minute:'2-digit'})}</div></div>`}).join('')||'<div class="empty">Будущих событий пока нет</div>';
}
function showToast(s){const t=$('toast');t.textContent=s;t.classList.add('show');setTimeout(()=>t.classList.remove('show'),3000)}
$('refresh').onclick=()=>{Android.refreshCalendar();Android.refreshBtc();showToast('Обновляю данные…')};
timer=setInterval(()=>{tick();if(nextEvent()&&nextEvent().timeMillis-Date.now()<3600000)Android.refreshBtc()},30000);
setTimeout(()=>Android.initialData(),250);
