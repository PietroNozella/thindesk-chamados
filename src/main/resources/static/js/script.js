// Alternar sidebar em telas pequenas
const menuButton = document.querySelector('.toggle-btn');
const sidebar = document.getElementById('sidebar');
if (menuButton && sidebar) {
    menuButton.addEventListener('click', () => {
        const expanded = sidebar.classList.toggle('active');
        menuButton.setAttribute('aria-expanded', String(expanded));
    });
}

// Alternar submenu
function toggleSubmenu(event, element) {
    event.preventDefault();
    element.parentElement.classList.toggle('open');
}
