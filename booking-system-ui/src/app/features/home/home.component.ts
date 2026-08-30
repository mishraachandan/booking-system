import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { ShowService, Show, Movie } from '../../core/services/show.service';
import { CityService, City } from '../../core/services/city.service';
import { FormsModule } from '@angular/forms';

const CITY_ID_KEY = 'selectedCityId';
const CITY_NAME_KEY = 'selectedCityName';

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="home page-enter">
      <!-- Hero -->
      <section class="hero">
        <div class="hero-bg"></div>
        <div class="container hero-content">
          <h1 class="hero-title">Your next <span class="accent">experience</span> awaits</h1>
          <p class="hero-subtitle">Book movie tickets, explore shows, and grab the best seats in town.</p>

          <div class="city-selector">
            <select
              [ngModel]="selectedCityId()"
              (ngModelChange)="onCityChange($event)"
              class="city-dropdown"
              aria-label="Select City">
              <option [value]="0">📍 Select your city</option>
              @for (city of cities; track city.id) {
                <option [value]="city.id">{{ city.name }}</option>
              }
            </select>
          </div>
        </div>
      </section>

      <!-- Movies Grid Section -->
      <section class="movies-section container">
        <div class="section-header">
          <h2>🎬 Now Showing</h2>
          <p class="text-muted">
            @if (!cityWarning && movies.length > 0) {
              {{ filteredMovies.length }} of {{ movies.length }} movies in {{ selectedCityName() || 'your city' }}
            } @else {
              No movies available
            }
          </p>
        </div>

        @if (loading) {
          <div class="movie-grid">
            @for (i of [1,2,3,4,5,6]; track i) {
              <div class="card movie-card-skeleton">
                <div class="skeleton poster-skeleton"></div>
                <div class="card-body">
                  <div class="skeleton" style="height: 20px; width: 70%; margin-bottom: 8px"></div>
                  <div class="skeleton" style="height: 14px; width: 40%"></div>
                </div>
              </div>
            }
          </div>
        } @else if (cityWarning) {
          <div class="city-warning">
            <span>📍</span>
            <p>Please select your city first to browse movies and book tickets.</p>
          </div>
        } @else if (movies.length === 0) {
          <div class="empty-state">
            <span class="empty-icon">🎭</span>
            <p>No movies found for {{ selectedCityName() || 'this city' }}. Check back later!</p>
          </div>
        } @else {
          <!-- Search & Filters -->
          <div class="search-filters">
            <div class="search-input-wrap">
              <span class="search-icon">🔍</span>
              <input 
                type="text" 
                [(ngModel)]="searchTerm" 
                placeholder="Search by title, genre, language..." 
                aria-label="Search movies" />
            </div>
            
            <div class="filter-row">
              <select [(ngModel)]="selectedGenre" aria-label="Filter by genre">
                <option value="">🎭 All Genres</option>
                @for (genre of genres; track genre) {
                  <option [value]="genre">{{ genre }}</option>
                }
              </select>

              <select [(ngModel)]="selectedLanguage" aria-label="Filter by language">
                <option value="">🗣️ All Languages</option>
                @for (lang of languages; track lang) {
                  <option [value]="lang">{{ lang }}</option>
                }
              </select>

              <select [(ngModel)]="sortBy" aria-label="Sort by">
                <option value="rating">★ Top Rated</option>
                <option value="duration">⏱️ Duration</option>
                <option value="title">🔤 Alphabetical</option>
              </select>
            </div>
          </div>

          @if (filteredMovies.length === 0) {
            <div class="empty-state">
              <span class="empty-icon">🔍</span>
              <p>No movies match your filters. Try adjusting them!</p>
            </div>
          } @else {
            <div class="movie-grid">
              @for (movie of filteredMovies; track movie.id) {
                <div class="card movie-card" (click)="selectMovie(movie)">
                  <div class="poster-wrap">
                    <img [src]="movie.posterUrl || 'https://placehold.co/300x450/1a1a2e/e23744?text=' + movie.title"
                         [alt]="movie.title" class="poster" loading="lazy" />
                    <div class="poster-overlay">
                      <span class="badge-lang">{{ movie.language || 'EN' }}</span>
                    </div>
                    <!-- Interactive Hover Details Overlay -->
                    <div class="card-interactive-overlay">
                      <div class="play-trigger">
                        <span class="play-arrow">▶</span>
                      </div>
                      <span class="overlay-action">Quick Book</span>
                      <span class="overlay-rating" *ngIf="movie.rating">★ {{ movie.rating }}/10</span>
                    </div>
                  </div>
                  <div class="card-body">
                    <h3 class="movie-title">{{ movie.title }}</h3>
                    <p class="movie-meta">
                      <span class="rating-badge" *ngIf="movie.rating">★ {{ movie.rating }}</span>
                      {{ movie.genre }} · {{ movie.durationMinutes }}m
                    </p>
                  </div>
                </div>
              }
            </div>
          }
        }
      </section>

      <!-- Shows for selected movie -->
      @if (selectedMovie) {
        <section class="shows-section container" id="shows">
          <div class="section-header">
            <h2>🕐 Showtimes for <span class="accent">{{ selectedMovie.title }}</span></h2>
            <button class="btn btn-ghost" (click)="selectedMovie = null; movieShows = []">✕ Close</button>
          </div>

          @if (movieShows.length === 0) {
            <div class="empty-state">
              <p>No showtimes available for this movie.</p>
            </div>
          } @else {
            <div class="shows-grid">
              @for (show of movieShows; track show.id) {
                <a [routerLink]="['/show', show.id, 'seats']" class="card show-card">
                  <div class="show-cinema">{{ show.screen && show.screen.cinema ? show.screen.cinema.name : 'Cinema' }}</div>
                  <div class="show-screen">{{ show.screen ? show.screen.name : 'Screen' }}</div>
                  <div class="show-time">{{ show.startTime | date:'hh:mm a' }}</div>
                  <div class="show-date">{{ show.startTime | date:'MMM d, yyyy' }}</div>
                </a>
              }
            </div>
          }
        </section>
      }
    </div>
  `,
  styles: [`
    .hero {
      position: relative;
      padding: 100px 0 80px;
      overflow: hidden;
    }
    .hero-bg {
      position: absolute;
      inset: 0;
      background: radial-gradient(ellipse at top center, var(--accent-glow), transparent 60%);
    }
    .hero-content { position: relative; text-align: center; }
    .hero-title {
      font-size: 54px;
      font-weight: 800;
      line-height: 1.15;
      margin-bottom: 20px;
      background: linear-gradient(135deg, var(--text-primary), var(--text-secondary));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero-subtitle {
      font-size: 20px;
      color: var(--text-secondary);
      margin-bottom: 36px;
      font-weight: 400;
    }
    .accent {
      background: var(--accent-gradient);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .text-muted { color: var(--text-muted); font-size: 14px; }

    .city-selector {
      display: inline-block;
    }
    .city-dropdown {
      padding: 16px 28px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      color: var(--text-primary);
      font-weight: 600;
      font-size: 16px;
      min-width: 280px;
      cursor: pointer;
      box-shadow: var(--shadow);
      transition: var(--transition);
      &:hover {
        border-color: var(--accent);
        box-shadow: 0 0 15px var(--accent-glow);
      }
    }

    .movies-section { padding: 40px 0 60px; }
    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 28px;
      h2 { font-size: 28px; font-weight: 700; }
    }

    .movie-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 32px;
    }

    .movie-card {
      cursor: pointer;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      overflow: hidden;
      perspective: 1000px;
      transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
      
      &:hover {
        transform: translateY(-8px) rotateX(4deg) rotateY(-2deg);
        border-color: var(--accent);
        box-shadow: 0 20px 35px rgba(0, 0, 0, 0.3), 0 0 15px var(--accent-glow);
      }
      
      .poster-wrap {
        position: relative;
        aspect-ratio: 2/3;
        overflow: hidden;
      }
      .poster {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.6s cubic-bezier(0.25, 0.8, 0.25, 1);
      }
      &:hover .poster { transform: scale(1.12); filter: brightness(0.7); }
      
      .card-interactive-overlay {
        position: absolute;
        inset: 0;
        background: linear-gradient(to top, rgba(10, 11, 20, 0.9) 10%, rgba(10, 11, 20, 0.4) 100%);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transform: translateY(20px);
        transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
        z-index: 5;
        padding: 20px;
        text-align: center;
      }
      &:hover .card-interactive-overlay {
        opacity: 1;
        transform: translateY(0);
      }
      
      .play-trigger {
        width: 54px;
        height: 54px;
        border-radius: 50%;
        background: var(--accent-gradient);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 12px;
        box-shadow: 0 0 15px var(--accent-glow);
        transition: all 0.3s ease;
        &:hover {
          transform: scale(1.15);
          box-shadow: 0 0 25px var(--accent);
        }
      }
      .play-arrow {
        color: white;
        font-size: 18px;
        margin-left: 3px;
      }
      .overlay-action {
        color: white;
        font-weight: 700;
        font-size: 15px;
        text-transform: uppercase;
        letter-spacing: 1px;
        margin-bottom: 4px;
      }
      .overlay-rating {
        color: #fbbf24;
        font-size: 13px;
        font-weight: 600;
      }

      .poster-overlay {
        position: absolute;
        top: 12px;
        left: 12px;
        z-index: 10;
      }
      .badge-lang {
        background: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(4px);
        -webkit-backdrop-filter: blur(4px);
        border: 1px solid rgba(255, 255, 255, 0.15);
        color: white;
        padding: 4px 10px;
        border-radius: 8px;
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
      }
      .card-body { padding: 18px; }
      .movie-title {
        font-size: 18px;
        font-weight: 700;
        margin-bottom: 6px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        color: var(--text-primary);
      }
      .movie-meta {
        font-size: 13px;
        color: var(--text-secondary);
        display: flex;
        align-items: center;
        gap: 8px;
      }
      .rating-badge {
        color: #fbbf24;
        font-weight: 700;
      }
    }

    .movie-card-skeleton {
      background: var(--bg-card);
      border: 1px solid var(--border);
      .poster-skeleton { aspect-ratio: 2/3; }
      .card-body { padding: 18px; }
    }

    .empty-state {
      text-align: center;
      padding: 80px 20px;
      color: var(--text-muted);
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      .empty-icon { font-size: 54px; display: block; margin-bottom: 20px; }
      p { font-size: 16px; font-weight: 500; }
    }

    .city-warning {
      text-align: center; padding: 60px 20px;
      background: rgba(245, 158, 11, 0.04);
      border: 1px dashed rgba(245, 158, 11, 0.25);
      border-radius: var(--radius);
      span { font-size: 48px; display: block; margin-bottom: 16px; }
      p { color: var(--warning); font-size: 18px; font-weight: 600; }
    }

    .shows-section {
      padding: 0 0 80px;
      scroll-margin-top: 80px;
    }
    .shows-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 20px;
    }
    .show-card {
      padding: 24px;
      text-align: center;
      cursor: pointer;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      .show-cinema {
        font-weight: 700;
        font-size: 16px;
        margin-bottom: 6px;
        color: var(--text-primary);
      }
      .show-screen {
        font-size: 13px;
        color: var(--text-muted);
        margin-bottom: 16px;
      }
      .show-time {
        font-size: 24px;
        font-weight: 800;
        color: var(--accent);
        margin-bottom: 6px;
      }
      .show-date {
        font-size: 13px;
        color: var(--text-secondary);
      }
      &:hover {
        border-color: var(--accent);
        box-shadow: 0 0 15px var(--accent-glow);
        transform: translateY(-4px);
      }
    }

    @media (max-width: 768px) {
      .hero-title { font-size: 38px; }
      .movie-grid { grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 20px; }
      .section-header { flex-direction: column; align-items: flex-start; gap: 8px; }
    }
  `]
})
export class HomeComponent implements OnInit {
  cities: City[] = [];
  shows: Show[] = [];
  movies: Movie[] = [];
  movieShows: Show[] = [];
  selectedMovie: Movie | null = null;
  loading = false;
  cityWarning = false;

  // Filter & Search states
  searchTerm = '';
  selectedGenre = '';
  selectedLanguage = '';
  sortBy = 'rating';

  genres: string[] = [];
  languages: string[] = [];

  get selectedCityId() { return this.cityService.selectedCityId; }
  get selectedCityName() { return this.cityService.selectedCityName; }

  constructor(
    private showService: ShowService,
    private cityService: CityService,
    private router: Router
  ) {}

  ngOnInit() {
    this.cityService.getCities().subscribe(c => {
      this.cities = c;
      if (this.selectedCityId() > 0) {
        this.loadShows();
      } else {
        this.cityWarning = true;
      }
    });
  }

  loadShows() {
    const cityId = this.selectedCityId();
    if (cityId === 0) {
      this.cityWarning = true;
      return;
    }
    this.cityWarning = false;
    this.loading = true;
    this.showService.getAllShows(cityId).subscribe({
      next: (shows) => {
        this.shows = shows;
        // Extract unique movies from shows
        const movieMap = new Map<number, Movie>();
        const genreSet = new Set<string>();
        const langSet = new Set<string>();

        shows.forEach(s => {
          if (s.movie) {
            if (!movieMap.has(s.movie.id)) {
              movieMap.set(s.movie.id, s.movie);
            }
            if (s.movie.genre) {
              s.movie.genre.split(',').map(g => g.trim()).forEach(g => {
                if (g) genreSet.add(g);
              });
            }
            if (s.movie.language) {
              s.movie.language.split(',').map(l => l.trim()).forEach(l => {
                if (l) langSet.add(l);
              });
            }
          }
        });

        this.movies = Array.from(movieMap.values());
        this.genres = Array.from(genreSet).sort();
        this.languages = Array.from(langSet).sort();
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get filteredMovies(): Movie[] {
    let list = [...this.movies];

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase().trim();
      list = list.filter(m => 
        (m.title && m.title.toLowerCase().includes(term)) ||
        (m.genre && m.genre.toLowerCase().includes(term)) ||
        (m.language && m.language.toLowerCase().includes(term))
      );
    }

    if (this.selectedGenre) {
      list = list.filter(m => m.genre && m.genre.toLowerCase().includes(this.selectedGenre.toLowerCase()));
    }

    if (this.selectedLanguage) {
      list = list.filter(m => m.language && m.language.toLowerCase().includes(this.selectedLanguage.toLowerCase()));
    }

    if (this.sortBy === 'rating') {
      list.sort((a, b) => (b.rating || 0) - (a.rating || 0));
    } else if (this.sortBy === 'duration') {
      list.sort((a, b) => (b.durationMinutes || 0) - (a.durationMinutes || 0));
    } else if (this.sortBy === 'title') {
      list.sort((a, b) => (a.title || '').localeCompare(b.title || ''));
    }

    return list;
  }

  onCityChange(newCityId: any) {
    const id = Number(newCityId);
    this.selectedMovie = null;
    this.movieShows = [];
    this.searchTerm = '';
    this.selectedGenre = '';
    this.selectedLanguage = '';

    const found = this.cities.find(c => c.id === id);
    if (found) {
      this.cityService.setCity(found.id, found.name);
      this.cityWarning = false;
      this.loadShows();
    } else {
      this.cityService.setCity(0, '');
      this.cityWarning = true;
      this.shows = [];
      this.movies = [];
      this.genres = [];
      this.languages = [];
    }
  }

  selectMovie(movie: Movie) {
    if (this.selectedCityId() === 0) {
      this.cityWarning = true;
      return;
    }
    // Primary UX: navigate to the movie detail page.
    this.router.navigate(['/movies', movie.id]);
  }
}

