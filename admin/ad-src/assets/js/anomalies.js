var anomalyLogs = []

$(async () => {
    if (!GetToken()) {
        location.href = '/login'
        return
    }

    $('.preloader').fadeIn(100)
    await loadAnomalyLogs()
    $('.preloader').fadeOut(500)
})

async function loadAnomalyLogs() {
    let res = await PenguRequestAPI(
        'GET',
        'api/anomalies',   // ✅ TUYỆT ĐỐI KHÔNG /anomalies
        {},
        {},
        true
    ).catch(e => {
        console.error(e)
        return null
    })

    if (!res || res.status !== 200) {
        Swal.fire("Lỗi", "Không load được anomaly logs", "error")
        return
    }

    anomalyLogs = await res.json()
    renderAnomalyLogs()
}

function renderAnomalyLogs() {
    const container = $('#body-container')
    container.empty()

    if (!anomalyLogs || anomalyLogs.length === 0) {
        container.html(`
            <tr>
                <td colspan="4" class="text-center">
                    Không có dữ liệu bất thường
                </td>
            </tr>
        `)
        return
    }

    anomalyLogs.forEach(log => {
        const row = $(`
            <tr>
                <td>${log.id?.substring(0,8)}...</td>
                <td>
                    <b class="text-warning">[${log.type}]</b><br>
                    <span class="text-danger">${log.description ?? 'N/A'}</span>
                </td>
                <td>${log.createdAt ? new Date(log.createdAt).toLocaleString('vi-VN') : 'N/A'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-info view-log">
                        Xem
                    </button>
                </td>
            </tr>
        `)

        row.find('.view-log').on('click', () => {
            Swal.fire('Chi tiết', log.description ?? '', 'info')
        })

        container.append(row)
    })
}
