import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { catalogApi, errorMessage } from '../api/client';
import DateStrip from '../components/DateStrip';
import { POSTER_FALLBACK } from '../utils';

export default function Home() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({ date: '', genre: '', city: '', theaterId: '', q: '' });
  const [movies, setMovies] = useState([]);
  const [genres, setGenres] = useState([]);
  const [cities, setCities] = useState([]);
  const [theaters, setTheaters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    catalogApi.genres().then(setGenres).catch(() => {});
    catalogApi.cities().then(setCities).catch(() => {});
    catalogApi.theaters().then(setTheaters).catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    setError('');
    const t = setTimeout(() => {
      catalogApi.movies(filters).then(setMovies).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
    }, 250);
    return () => clearTimeout(t);
  }, [filters]);

  const set = (k, v) => setFilters((f) => ({ ...f, [k]: v, ...(k === 'city' ? { theaterId: '' } : {}) }));
  const shownTheaters = theaters.filter((t) => !filters.city || t.city === filters.city);
  const openMovie = (m) => navigate(`/movies/${m.id}${filters.date ? `?date=${filters.date}` : ''}`);

  return (
    <>
      <section className="hero p-4 p-md-5 mb-4">
        <h1 className="fw-bold">Book your movie tickets</h1>
        <p className="lead mb-0">Pick a movie, choose your seats and you're in. 🍿</p>
      </section>

      <div className="card border-0 shadow-sm p-3 mb-4">
        <DateStrip value={filters.date} onChange={(v) => set('date', v)} allowAll />
        <div className="row g-2 mt-2">
          <div className="col-md-3"><input className="form-control" placeholder="Search movies…" value={filters.q} onChange={(e) => set('q', e.target.value)} /></div>
          <div className="col-md-3">
            <select className="form-select" value={filters.genre} onChange={(e) => set('genre', e.target.value)}>
              <option value="">All genres</option>
              {genres.map((g) => <option key={g}>{g}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <select className="form-select" value={filters.city} onChange={(e) => set('city', e.target.value)}>
              <option value="">All cities</option>
              {cities.map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>
          <div className="col-md-3">
            <select className="form-select" value={filters.theaterId} onChange={(e) => set('theaterId', e.target.value)}>
              <option value="">All theaters</option>
              {shownTheaters.map((t) => <option key={t.id} value={t.id}>{t.name} ({t.city})</option>)}
            </select>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <p className="text-muted">Loading movies… (the server may take up to a minute to wake up)</p>}
      {!loading && movies.length === 0 && !error && <div className="text-center text-muted py-5">No movies match your filters.</div>}

      <div className="row g-4">
        {movies.map((m) => (
          <div className="col-6 col-md-4 col-lg-3" key={m.id}>
            <div className="card h-100 border-0 shadow-sm movie-card" onClick={() => openMovie(m)}>
              <img src={m.posterUrl || POSTER_FALLBACK} onError={(e) => { e.currentTarget.src = POSTER_FALLBACK; }} className="card-img-top poster" alt={m.title} />
              <div className="card-body">
                <h6 className="card-title mb-1">{m.title}</h6>
                <div className="small text-muted">{m.genre} · {m.language}</div>
                <div className="small mt-1">
                  {m.rating != null && <span className="me-2">⭐ {m.rating}/10</span>}
                  {m.certificate && <span className="badge bg-secondary">{m.certificate}</span>}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
