import { isoDate, nextDays } from '../utils';

/** Horizontal list of the next 7 days. value = 'YYYY-MM-DD' or '' for any day. */
export default function DateStrip({ value, onChange, allowAll = false }) {
  return (
    <div className="d-flex gap-2 overflow-auto pb-1">
      {allowAll && (
        <button type="button" className={`btn date-chip ${value === '' ? 'btn-dark' : 'btn-outline-secondary'}`} onClick={() => onChange('')}>
          <div className="day">Any</div><div className="num">∞</div>
        </button>
      )}
      {nextDays(7).map((d, i) => {
        const iso = isoDate(d);
        return (
          <button key={iso} type="button" className={`btn date-chip ${value === iso ? 'btn-dark' : 'btn-outline-secondary'}`} onClick={() => onChange(iso)}>
            <div className="day">{i === 0 ? 'Today' : d.toLocaleDateString('en-IN', { weekday: 'short' })}</div>
            <div className="num">{d.getDate()}</div>
            <div className="day">{d.toLocaleDateString('en-IN', { month: 'short' })}</div>
          </button>
        );
      })}
    </div>
  );
}
