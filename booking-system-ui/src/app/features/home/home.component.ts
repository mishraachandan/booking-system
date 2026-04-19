import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ShowService, Show, Movie } from '../../core/services/show.service';
import { CityService, City } from '../../core/services/city.service';
import { FormsModule } from '@angular/forms';

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
            <select [(ngModel)]="selectedCityId" (change)="onCityChange()" class="city-dropdown">
              <option [value]="0">📍 Select your city</option>
              @for (city of cities; track city.id) {
                <option [value]="city.id">{{ city.name }}</option>
              }
            </select>
          </div>
        </div>
      </section>

      <!-- Movies Grid -->
      <section class="movies-section container">
        <div class="section-header">
          <h2>🎬 Now Showing</h2>
          <p class="text-muted">{{ movies.length }} movies available</p>
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
            <p>No movies found for this city. Check back later!</p>
          </div>
        } @else {
          <div class="movie-grid">
            @for (movie of movies; track movie.id) {
              <div class="card movie-card" (click)="selectMovie(movie)">
                <div class="poster-wrap">
                  <img [src]="movie.posterUrl || 'https://placehold.co/300x450/1a1a2e/e23744?text=' + movie.title"
                       [alt]="movie.title" class="poster" />
                  <div class="poster-overlay">
                    <span class="badge-lang">{{ movie.language || 'EN' }}</span>
                  </div>
                </div>
                <div class="card-body">
                  <h3 class="movie-title">{{ movie.title }}</h3>
                  <p class="movie-meta">{{ movie.genre }} · {{ movie.durationMinutes }}min</p>
                </div>
              </div>
            }
          </div>
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
                  <div class="show-cinema">{{ show.screen?.cinema?.name || 'Cinema' }}</div>
                  <div class="show-screen">{{ show.screen?.name || 'Screen' }}</div>
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
      padding: 80px 0 60px;
      overflow: hidden;
    }
    .hero-bg {
      position: absolute;
      inset: 0;
      background: radial-gradient(ellipse at top center, rgba(226, 55, 68, 0.15), transparent 60%);
    }
    .hero-content { position: relative; text-align: center; }
    .hero-title {
      font-size: 48px;
      font-weight: 800;
      line-height: 1.15;
      margin-bottom: 16px;
    }
    .hero-subtitle {
      font-size: 18px;
      color: var(--text-secondary);
      margin-bottom: 32px;
    }
    .accent { color: var(--accent); }
    .text-muted { color: var(--text-muted); font-size: 14px; }

    .city-selector {
      display: inline-block;
    }
    .city-dropdown {
      padding: 14px 24px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      color: var(--text-primary);
      font-size: 16px;
      min-width: 260px;
      cursor: pointer;
      transition: var(--transition);
      &:hover { border-color: var(--accent); }
    }

    .movies-section { padding: 40px 0 60px; }
    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 28px;
      h2 { font-size: 24px; font-weight: 700; }
    }

    .movie-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 24px;
    }

    .movie-card {
      cursor: pointer;
      .poster-wrap {
        position: relative;
        aspect-ratio: 2/3;
        overflow: hidden;
        border-radius: var(--radius) var(--radius) 0 0;
      }
      .poster {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.4s ease;
      }
      &:hover .poster { transform: scale(1.05); }
      .poster-overlay {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        padding: 8px 12px;
        background: linear-gradient(transparent, rgba(0,0,0,0.8));
      }
      .badge-lang {
        background: var(--accent);
        color: white;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 11px;
        font-weight: 600;
        text-transform: uppercase;
      }
      .card-body { padding: 14px; }
      .movie-title {
        font-size: 16px;
        font-weight: 600;
        margin-bottom: 4px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .movie-meta { font-size: 13px; color: var(--text-muted); }
    }

    .movie-card-skeleton {
      .poster-skeleton { aspect-ratio: 2/3; }
      .card-body { padding: 14px; }
    }

    .empty-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
      .empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
    }

    .city-warning {
      text-align: center; padding: 48px 20px;
      background: rgba(243, 156, 18, 0.06);
      border: 1px dashed rgba(243, 156, 18, 0.3);
      border-radius: var(--radius);
      span { font-size: 40px; display: block; margin-bottom: 12px; }
      p { color: #f39c12; font-size: 16px; font-weight: 500; }
    }

    .shows-section {
      padding: 0 0 60px;
      scroll-margin-top: 80px;
    }
    .shows-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
      gap: 16px;
    }
    .show-card {
      padding: 20px;
      text-align: center;
      cursor: pointer;
      .show-cinema {
        font-weight: 600;
        font-size: 15px;
        margin-bottom: 4px;
      }
      .show-screen {
        font-size: 13px;
        color: var(--text-muted);
        margin-bottom: 12px;
      }
      .show-time {
        font-size: 22px;
        font-weight: 700;
        color: var(--accent);
        margin-bottom: 4px;
      }
      .show-date {
        font-size: 13px;
        color: var(--text-secondary);
      }
    }

    @media (max-width: 768px) {
      .hero-title { font-size: 32px; }
      .movie-grid { grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 16px; }
    }
  `]
})
export class HomeComponent implements OnInit {
  cities: City[] = [];
  selectedCityId = 0;
  shows: Show[] = [];
  movies: Movie[] = [];
  movieShows: Show[] = [];
  selectedMovie: Movie | null = null;
  loading = false;
  cityWarning = true;

  constructor(
    private showService: ShowService,
    private cityService: CityService
  ) {}

  ngOnInit() {
    this.cityService.getCities().subscribe(c => this.cities = c);
    // Don't load shows until a city is selected
  }

  loadShows() {
    this.loading = true;
    this.showService.getAllShows(this.selectedCityId).subscribe({
      next: (shows) => {
        this.shows = shows;
        // Extract unique movies from shows
        const movieMap = new Map<number, Movie>();
        shows.forEach(s => {
          if (s.movie && !movieMap.has(s.movie.id)) {
            movieMap.set(s.movie.id, s.movie);
          }
        });
        this.movies = Array.from(movieMap.values());
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onCityChange() {
    this.selectedMovie = null;
    this.movieShows = [];
    this.cityWarning = this.selectedCityId == 0;
    if (!this.cityWarning) {
      this.loadShows();
    } else {
      this.shows = [];
      this.movies = [];
    }
  }

  selectMovie(movie: Movie) {
    if (this.selectedCityId == 0) {
      this.cityWarning = true;
      return;
    }
    this.selectedMovie = movie;
    // Filter shows for this movie that belong to the selected city
    this.movieShows = this.shows.filter(s => s.movie?.id === movie.id);
    setTimeout(() => document.getElementById('shows')?.scrollIntoView({ behavior: 'smooth' }), 100);
  }
}
