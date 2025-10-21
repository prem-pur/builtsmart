// BuildSmart CMS - Professional Landing Page JavaScript

// Initialize AOS with professional settings
document.addEventListener('DOMContentLoaded', function() {
    AOS.init({
        duration: 600,
        easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
        once: true,
        offset: 80,
        delay: 50,
        disable: 'mobile'
    });
    
    // Refresh AOS on font load for better positioning
    document.fonts.ready.then(() => AOS.refresh());
});

// Navbar scroll effect with throttling for performance
const navbar = document.getElementById('navbar');
const scrollTop = document.getElementById('scrollTop');
let ticking = false;

function updateScrollState() {
    const scrolled = window.scrollY > 50;
    navbar.classList.toggle('scrolled', scrolled);
    scrollTop.classList.toggle('show', window.scrollY > 300);
    ticking = false;
}

window.addEventListener('scroll', function() {
    if (!ticking) {
        window.requestAnimationFrame(updateScrollState);
        ticking = true;
    }
}, { passive: true });

// Mobile menu toggle
const hamburger = document.getElementById('hamburger');
const navMenu = document.getElementById('nav-menu');

if (hamburger) {
    hamburger.addEventListener('click', function() {
        navMenu.classList.toggle('active');
        hamburger.classList.toggle('active');
    });
}

// Close mobile menu when clicking on a nav link
const navLinks = document.querySelectorAll('.nav-link');
navLinks.forEach(link => {
    link.addEventListener('click', function() {
        navMenu.classList.remove('active');
        hamburger.classList.remove('active');
    });
});

// Scroll to top button
if (scrollTop) {
    scrollTop.addEventListener('click', function() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });
}

// Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function(e) {
        const href = this.getAttribute('href');
        if (href !== '#' && href.length > 1) {
            e.preventDefault();
            const target = document.querySelector(href);
            if (target) {
                const offsetTop = target.offsetTop - 80;
                window.scrollTo({
                    top: offsetTop,
                    behavior: 'smooth'
                });
            }
        }
    });
});

// Contact form submission
const contactForm = document.querySelector('.contact-form form');
if (contactForm) {
    contactForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Get form data
        const formData = new FormData(this);
        
        // Show loading state
        const submitBtn = this.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';
        submitBtn.disabled = true;
        
        // Simulate form submission (replace with actual API call)
        setTimeout(() => {
            // Success message
            alert('Thank you for your message! We will get back to you soon.');
            
            // Reset form
            this.reset();
            
            // Restore button
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }, 1500);
    });
}

// Add parallax effect to hero section
window.addEventListener('scroll', function() {
    const scrolled = window.pageYOffset;
    const heroContent = document.querySelector('.hero-content');
    const constructionSilhouette = document.querySelector('.construction-silhouette');
    
    if (heroContent) {
        heroContent.style.transform = `translateY(${scrolled * 0.5}px)`;
    }
    
    if (constructionSilhouette) {
        constructionSilhouette.style.transform = `translateY(${scrolled * 0.3}px)`;
    }
});

// Counter animation for stats
const animateCounters = () => {
    const counters = document.querySelectorAll('.stat h3');
    const speed = 200;
    
    counters.forEach(counter => {
        const target = counter.innerText;
        const isPercentage = target.includes('%');
        const isPlusSign = target.includes('+');
        const isKSign = target.includes('K');
        
        // Extract number
        let num = parseInt(target.replace(/\D/g, ''));
        
        const updateCount = () => {
            const inc = num / speed;
            const current = parseInt(counter.innerText.replace(/\D/g, ''));
            
            if (current < num) {
                let newValue = Math.ceil(current + inc);
                if (isKSign) {
                    counter.innerText = (newValue / 1000).toFixed(1) + 'K' + (isPlusSign ? '+' : '');
                } else if (isPercentage) {
                    counter.innerText = (newValue / 10).toFixed(1) + '%';
                } else {
                    counter.innerText = newValue + (isPlusSign ? '+' : '');
                }
                setTimeout(updateCount, 1);
            } else {
                counter.innerText = target;
            }
        };
        
        updateCount();
    });
};

// Trigger counter animation when stats are in view
const observerOptions = {
    threshold: 0.5,
    rootMargin: '0px'
};

const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            animateCounters();
            observer.unobserve(entry.target);
        }
    });
}, observerOptions);

const statsSection = document.querySelector('.hero-stats');
if (statsSection) {
    observer.observe(statsSection);
}

// Add hover effect to service cards
const serviceCards = document.querySelectorAll('.service-card');
serviceCards.forEach(card => {
    card.addEventListener('mouseenter', function() {
        this.style.transform = 'translateY(-8px)';
    });
    
    card.addEventListener('mouseleave', function() {
        this.style.transform = 'translateY(0)';
    });
});

// Add active class to current nav item
const currentLocation = location.hash || '#home';
navLinks.forEach(link => {
    if (link.getAttribute('href') === currentLocation) {
        link.classList.add('active');
    }
});

// Update active nav item on scroll
window.addEventListener('scroll', () => {
    let current = '';
    const sections = document.querySelectorAll('section[id]');
    
    sections.forEach(section => {
        const sectionTop = section.offsetTop;
        const sectionHeight = section.clientHeight;
        
        if (window.pageYOffset >= sectionTop - 200) {
            current = section.getAttribute('id');
        }
    });
    
    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === `#${current}`) {
            link.classList.add('active');
        }
    });
});

console.log('BuildSmart CMS Landing Page Loaded Successfully! 🚀');
