import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Movie } from '../../core/services/show.service';
import { MovieService } from '../../core/services/movie.service';
import { ShowService, Show } from '../../core/services/show.service';

@Component({
  selector: 'app-movie-detail',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="movie-detail-page page-enter">
      @if (loading) {
        <div class="loading-state"><div class="spinner"></div><p>Loading movie...</p></div>
      } @else if (error) {
        <div class="error-box">
          <p>{{ error }}</p>
          <a routerLink="/" class="btn btn-outline">Back to Home</a>
        </div>
      } @else if (movie) {
        <!-- Hero banner -->
        <section class="hero-banner" [style.background-image]="'url(' + movie.posterUrl + ')'">
          <div class="hero-overlay"></div>
          <div class="container hero-content">
            <div class="poster-wrap" (click)="openZoomModal()">
              <img [src]="movie.posterUrl || fallbackPoster" [alt]="movie.title" class="poster" />
              <div class="poster-zoom-overlay">
                <span class="zoom-icon">🔍</span>
                <span>Click to Expand</span>
              </div>
            </div>
            <div class="hero-info">
              <h1 class="title">{{ movie.title }}</h1>
              <div class="meta-row">
                @if (movie.rating) {
                  <span class="rating">★ {{ movie.rating }}/10</span>
                }
                <span class="chip">{{ movie.language }}</span>
                <span class="chip">{{ movie.genre }}</span>
                <span class="chip">{{ movie.durationMinutes }} min</span>
                @if (movie.releaseDate) {
                  <span class="chip">{{ movie.releaseDate | date:'mediumDate' }}</span>
                }
              </div>
              <p class="synopsis">{{ movie.description }}</p>
              <div class="actions">
                <button class="btn btn-primary" (click)="scrollToShows()" [disabled]="showsLoading">
                  🎟 Book Tickets
                </button>
                @if (trailerSafeUrl) {
                  <button class="btn btn-outline" (click)="toggleTrailer()">
                    ▶ Watch Trailer
                  </button>
                }
              </div>
            </div>
          </div>
        </section>

        <!-- Trailer Lightbox Modal -->
        @if (trailerOpen && trailerSafeUrl) {
          <div class="lightbox-backdrop" (click)="toggleTrailer()">
            <div class="lightbox-content" (click)="$event.stopPropagation()">
              <button class="lightbox-close" (click)="toggleTrailer()" aria-label="Close trailer">✕</button>
              <iframe
                [src]="trailerSafeUrl"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowfullscreen>
              </iframe>
            </div>
          </div>
        }

        <!-- Poster Zoom Modal -->
        @if (zoomModalOpen) {
          <div class="zoom-modal-backdrop" (click)="closeZoomModal()">
            <div class="zoom-modal-content" (click)="$event.stopPropagation()">
              <button class="zoom-modal-close" (click)="closeZoomModal()" aria-label="Close image">✕</button>
              
              <div class="zoom-img-container" [style.transform]="'scale(' + zoomScale + ') translate(' + panX + 'px, ' + panY + 'px)'"
                   (mousedown)="startPan($event)" (mousemove)="onPan($event)" (mouseup)="endPan()" (mouseleave)="endPan()">
                <img [src]="movie.posterUrl || fallbackPoster" [alt]="movie.title" class="zoomed-poster" draggable="false" />
              </div>

              <!-- Interactive Zoom Controls -->
              <div class="zoom-controls">
                <button class="btn-zoom" (click)="zoomIn()" aria-label="Zoom in">+</button>
                <span class="zoom-value">{{ (zoomScale * 100) | number:'1.0-0' }}%</span>
                <button class="btn-zoom" (click)="zoomOut()" aria-label="Zoom out">−</button>
                <button class="btn-zoom reset" (click)="resetZoom()">Reset</button>
              </div>
            </div>
          </div>
        }

        <!-- Cast -->
        @if (movie.cast && movie.cast.length > 0) {
          <section class="cast-section container">
            <h2>Cast</h2>
            <div class="cast-list">
              @for (actor of movie.cast; track actor) {
                <div class="cast-chip">
                  <div class="cast-avatar">{{ initialsOf(actor) }}</div>
                  <span>{{ actor }}</span>
                </div>
              }
            </div>
          </section>
        }

        <!-- Showtimes -->
        <section class="shows-section container" id="shows">
          <h2>Showtimes</h2>
          @if (showsLoading) {
            <p class="text-muted">Loading showtimes...</p>
          } @else if (shows.length === 0) {
            <div class="empty-state">
              <p>No showtimes available for this movie right now.</p>
            </div>
          } @else {
            <div class="shows-grid">
              @for (show of shows; track show.id) {
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

        <!-- Sticky Floating Booking Bar -->
        <div class="sticky-booking-bar" [class.visible]="showStickyBar">
          <div class="container" style="display: flex; align-items: center; justify-content: space-between; width: 100%;">
            <div class="bar-info">
              <span class="bar-title">{{ movie.title }}</span>
              <span class="bar-meta">★ {{ movie.rating }}/10 · {{ movie.language }} · {{ movie.genre }}</span>
            </div>
            <button class="btn btn-primary" (click)="scrollToShows()">
              🎟 Book Now
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .movie-detail-page { padding-bottom: 80px; }
    .loading-state, .error-box {
      text-align: center; padding: 120px 20px; color: var(--text-muted);
      p { margin: 16px 0; }
    }
    .spinner {
      width: 48px; height: 48px; border: 3px solid var(--border);
      border-top-color: var(--accent); border-radius: 50%;
      animation: spin 0.8s linear infinite; margin: 0 auto;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .hero-banner {
      position: relative;
      background-size: cover; background-position: center;
      padding: 80px 0 60px;
    }
    .hero-overlay {
      position: absolute; inset: 0;
      background: linear-gradient(135deg, rgba(10, 11, 20, 0.95), rgba(10, 11, 20, 0.75));
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
    }
    .hero-content {
      position: relative;
      display: flex; gap: 40px; align-items: center;
    }
    .poster-wrap {
      flex: 0 0 260px;
      position: relative;
      cursor: zoom-in;
      perspective: 1000px;
      
      &:hover .poster {
        transform: scale(1.04) rotateY(-5deg) rotateX(5deg);
        box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);
      }
      
      .poster {
        width: 100%; aspect-ratio: 2 / 3; object-fit: cover;
        border-radius: var(--radius); box-shadow: var(--shadow-lg);
        border: 1px solid rgba(255, 255, 255, 0.1);
        transition: transform 0.5s cubic-bezier(0.25, 0.8, 0.25, 1), box-shadow 0.5s ease;
      }
      
      .poster-zoom-overlay {
        position: absolute;
        inset: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 8px;
        color: white;
        font-weight: 600;
        font-size: 13px;
        opacity: 0;
        transition: all 0.3s ease;
        border-radius: var(--radius);
      }
      &:hover .poster-zoom-overlay {
        opacity: 1;
      }
      .zoom-icon {
        font-size: 24px;
      }
    }

    /* Zoom Lightbox Modal */
    .zoom-modal-backdrop {
      position: fixed; inset: 0; z-index: 250;
      background: rgba(10, 11, 20, 0.95);
      display: flex; align-items: center; justify-content: center;
    }
    .zoom-modal-content {
      position: relative;
      max-width: 90vw; max-height: 90vh;
      display: flex; flex-direction: column; align-items: center;
    }
    .zoom-modal-close {
      position: absolute; top: -50px; right: 0;
      background: none; border: none; color: white;
      font-size: 28px; cursor: pointer; opacity: 0.7;
      transition: opacity 0.2s;
      &:hover { opacity: 1; }
    }
    .zoom-img-container {
      cursor: grab;
      display: flex; align-items: center; justify-content: center;
      transition: transform 0.1s ease-out;
      user-select: none;
      -webkit-user-drag: none;
      &:active { cursor: grabbing; }
    }
    .zoomed-poster {
      max-height: 70vh; max-width: 100%;
      border-radius: var(--radius);
      box-shadow: 0 30px 70px rgba(0,0,0,0.6);
      border: 1px solid rgba(255,255,255,0.1);
    }
    .zoom-controls {
      display: flex; align-items: center; gap: 14px;
      background: rgba(255,255,255,0.08);
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
      border: 1px solid rgba(255,255,255,0.15);
      padding: 10px 20px; border-radius: 30px; margin-top: 24px;
      box-shadow: 0 10px 30px rgba(0,0,0,0.3);
    }
    .btn-zoom {
      background: none; border: none; color: white;
      font-size: 20px; font-weight: 700; width: 36px; height: 36px;
      display: flex; align-items: center; justify-content: center;
      cursor: pointer; border-radius: 50%;
      transition: background 0.2s;
      &:hover { background: rgba(255,255,255,0.1); }
      &.reset { font-size: 13px; font-weight: 600; width: auto; padding: 0 12px; border-radius: 20px; }
    }
    .zoom-value {
      color: var(--text-primary); font-size: 14px; font-weight: 700;
      min-width: 48px; text-align: center;
    }
    .hero-info { flex: 1; min-width: 0; }
    .title {
      font-size: 44px;
      font-weight: 800;
      margin-bottom: 16px;
      line-height: 1.15;
    }
    .meta-row {
      display: flex; flex-wrap: wrap; gap: 10px; align-items: center; margin-bottom: 24px;
    }
    .rating {
      background: rgba(245, 158, 11, 0.15); color: #fbbf24;
      padding: 6px 14px; border-radius: 20px;
      font-weight: 700; font-size: 13px;
    }
    .chip {
      background: rgba(255,255,255,0.08); color: var(--text-secondary);
      padding: 6px 14px; border-radius: 20px; font-size: 13px;
      border: 1px solid var(--border);
    }
    .synopsis {
      color: var(--text-secondary); font-size: 16px; line-height: 1.6;
      max-width: 760px; margin-bottom: 32px;
    }
    .actions { display: flex; gap: 14px; flex-wrap: wrap; }

    .cast-section, .shows-section {
      padding: 40px 0;
      h2 { font-size: 24px; font-weight: 700; margin-bottom: 24px; }
    }
    .cast-list { display: flex; flex-wrap: wrap; gap: 16px; }
    .cast-chip {
      display: flex; align-items: center; gap: 12px;
      background: var(--bg-card); border: 1px solid var(--border);
      border-radius: 999px; padding: 8px 18px 8px 8px;
      font-size: 14px;
      font-weight: 500;
      transition: var(--transition);
      &:hover {
        border-color: var(--accent);
        transform: scale(1.05);
      }
    }
    .cast-avatar {
      width: 36px; height: 36px; border-radius: 50%;
      background: var(--accent-gradient); color: white;
      display: flex; align-items: center; justify-content: center;
      font-weight: 700; font-size: 13px;
    }

    .shows-grid {
      display: grid; gap: 20px;
      grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    }
    .show-card {
      padding: 24px; transition: var(--transition);
      text-decoration: none; color: inherit;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius);
      &:hover {
        transform: translateY(-4px);
        border-color: var(--accent);
        box-shadow: 0 0 15px var(--accent-glow);
      }
    }
    .show-cinema { font-weight: 700; font-size: 16px; margin-bottom: 4px; color: var(--text-primary); }
    .show-screen { font-size: 13px; color: var(--text-muted); margin-bottom: 12px; }
    .show-time { font-size: 24px; font-weight: 800; color: var(--accent); }
    .show-date { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

    .empty-state {
      text-align: center; padding: 60px 20px; color: var(--text-muted);
      background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius);
    }

    @media (max-width: 768px) {
      .hero-content { flex-direction: column; gap: 24px; text-align: center; }
      .poster-wrap { flex: 0 0 auto; max-width: 220px; }
      .title { font-size: 32px; }
      .meta-row { justify-content: center; }
      .synopsis { margin-left: auto; margin-right: auto; }
      .actions { justify-content: center; }
    }
  `]
})
export class MovieDetailComponent implements OnInit, OnDestroy {
  movie: Movie | null = null;
  shows: Show[] = [];
  loading = true;
  showsLoading = true;
  error = '';
  trailerOpen = false;
  trailerSafeUrl: SafeResourceUrl | null = null;
  fallbackPoster = 'https://placehold.co/300x450/1a1a2e/e23744?text=Movie';
  showStickyBar = false;

  // Poster Zoom / Lightbox States
  zoomModalOpen = false;
  zoomScale = 1.0;
  panX = 0;
  panY = 0;
  private isPanning = false;
  private startX = 0;
  private startY = 0;

  openZoomModal() {
    this.zoomModalOpen = true;
    this.zoomScale = 1.0;
    this.panX = 0;
    this.panY = 0;
  }

  closeZoomModal() {
    this.zoomModalOpen = false;
  }

  zoomIn() {
    if (this.zoomScale < 3.0) {
      this.zoomScale = parseFloat((this.zoomScale + 0.25).toFixed(2));
    }
  }

  zoomOut() {
    if (this.zoomScale > 0.5) {
      this.zoomScale = parseFloat((this.zoomScale - 0.25).toFixed(2));
    }
  }

  resetZoom() {
    this.zoomScale = 1.0;
    this.panX = 0;
    this.panY = 0;
  }

  startPan(event: MouseEvent) {
    this.isPanning = true;
    this.startX = event.clientX - this.panX;
    this.startY = event.clientY - this.panY;
  }

  onPan(event: MouseEvent) {
    if (this.isPanning) {
      this.panX = event.clientX - this.startX;
      this.panY = event.clientY - this.startY;
    }
  }

  endPan() {
    this.isPanning = false;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private movieService: MovieService,
    private showService: ShowService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const movieId = Number(this.route.snapshot.paramMap.get('movieId'));
    if (!movieId) {
      this.error = 'Invalid movie id.';
      this.loading = false;
      return;
    }
    this.movieService.getMovie(movieId).subscribe({
      next: (m) => {
        this.movie = m;
        if (m.trailerUrl) {
          this.trailerSafeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(m.trailerUrl);
        }
        this.loading = false;
      },
      error: () => {
        this.error = 'Movie not found.';
        this.loading = false;
      }
    });

    this.showService.getShowsByMovie(movieId).subscribe({
      next: (s) => {
        // Only future shows
        const now = Date.now();
        this.shows = s.filter(x => new Date(x.startTime).getTime() > now)
          .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime());
        this.showsLoading = false;
      },
      error: () => { this.showsLoading = false; }
    });
  }

  ngOnDestroy() {
    // Scroll event listener is automatically handled by Angular's host binding
  }

  @HostListener('window:scroll', [])
  onWindowScroll() {
    if (typeof window !== 'undefined') {
      const scrollPos = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0;
      this.showStickyBar = scrollPos > 450;
    }
  }

  toggleTrailer() { this.trailerOpen = !this.trailerOpen; }

  scrollToShows() {
    setTimeout(() => document.getElementById('shows')?.scrollIntoView({ behavior: 'smooth' }), 50);
  }

  initialsOf(name: string): string {
    return name.split(/\s+/).filter(Boolean).slice(0, 2).map(s => s[0]).join('').toUpperCase();
  }
}
