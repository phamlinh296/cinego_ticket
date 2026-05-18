const today = new Date().toISOString().split('T')[0]   // ngày UTC (giống Spark)
const currentHourUTC = new Date().getUTCHours()        // giờ UTC để query Redis key đúng

$(async () => {
    $('#dashboard-date').text('Ngày: ' + today)
    $('.preloader').fadeIn(100)
    await Promise.all([loadTopMovies(), loadRevenue()])
    $('.preloader').fadeOut(500)
})

async function loadTopMovies() {
    const movieIds = await PenguRequestAPI('GET', `analytics/top-movies?date=${today}`, {}, {}, false)
        .then(r => r.ok ? r.json() : [])
        .catch(() => [])

    const container = $('#top-movies-container')
    container.empty()

    if (!movieIds || movieIds.length === 0) {
        container.html('<tr><td colspan="3" class="text-center">Chưa có dữ liệu — hãy gửi vài event trước</td></tr>')
        return
    }

    let rank = 1
    for (const movieId of movieIds) {
        const movie = await PenguRequestAPI('GET', `api/movie/${movieId}`, {}, {}, false)
            .then(r => r.ok ? r.json() : null)
            .catch(() => null)

        const title = movie ? movie.title : `Movie #${movieId}`
        container.append(`<tr><td>${rank++}</td><td>${title}</td><td class="text-muted">${movieId}</td></tr>`)
    }
}

async function loadRevenue() {
    const container = $('#revenue-container')
    container.empty()

    // Query tất cả giờ UTC 0-23, hiển thị giờ VN (+7), chỉ show giờ có data
    let hasData = false

    for (let utcH = 0; utcH <= 23; utcH++) {
        const revenue = await PenguRequestAPI('GET', `analytics/revenue?date=${today}&hour=${utcH}`, {}, {}, false)
            .then(r => r.ok ? r.text() : null)
            .catch(() => null)

        if (!revenue || revenue === 'null') continue

        hasData = true
        const vnH = (utcH + 7) % 24
        const vnH1 = (utcH + 8) % 24
        const isNow = utcH === currentHourUTC
        const highlight = isNow ? 'class="text-warning"' : ''
        const formatted = parseFloat(revenue).toLocaleString('vi-VN') + ' ₫'
        container.append(`<tr><td ${highlight}>${String(vnH).padStart(2,'0')}:00 – ${String(vnH1).padStart(2,'0')}:00</td><td ${highlight}>${formatted}</td></tr>`)
    }

    if (!hasData) {
        container.append('<tr><td colspan="2" class="text-center text-muted">Chưa có dữ liệu — hãy gửi vài event trước</td></tr>')
    }
}
