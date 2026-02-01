$(async function () {
    $("#list-movies").empty()

    let list = await PenguRequestAPI('GET', 'api/movie/getall?pageSize=10', {}, {}, false).then(r => r.json()).catch(error => { console.log(error); return false })

    console.log(list)

    list.forEach(element => {
        let item = $(
            `<div class="item">
                <div class="movie-grid">
                    <div class="movie-thumb c-thumb">
                        <a href="index-2.html#0">
                            <img id="movie-img" src="assets/images/movie/movie01.jpg" alt="movie">
                        </a>
                    </div>
                    <div class="movie-content bg-one">
                        <h5 class="title m-0"> 
                            <a id="movie-title" href="index-2.html#0">alone</a>
                        </h5>
                        <ul class="movie-rating-percent">
                            <span class="content" id="movie-info"></span>
                            <span class="content" id="movie-info-2"></span>
                        </ul>
                    </div>
                </div> 
            </div>`
        )

        item.find('#movie-img').attr('src', element.image)
        item.find('#movie-title').text(element.title)
        item.find('#movie-info').text(`Thời lượng: ${GetTimeString(element.durationInMins)}`)
        item.find('#movie-info-2').text(`Thể loại: ${element.genres.map(d => d.genre).join(", ")}`)
        item.find('a').attr('href', `movie-details.html?movie-id=${encodeURI(element.id)}`)

        $("#list-movies").append(item)
    });

    // 2. Xử lý danh sách PHIM GỢI Ý
    const isLogged = await TokenIsVaild();
    let urlRecommendation = 'api/recommendations';

    if (isLogged) {
        let uid = GetUserIdFromToken();
        if (uid) urlRecommendation += `?userId=${uid}`;
    }

    let recList = await PenguRequestAPI('GET', urlRecommendation, {}, {}, isLogged)
        .then(r => r.ok ? r.json() : [])
        .catch(error => { console.log("Lỗi gợi ý:", error); return [] });

    if (recList && recList.length > 0) {
        $("#recommendation-section").show(); 
        $("#list-recommended-movies").empty();

        recList.forEach(element => {
            let item = createMovieItem(element);
            $("#list-recommended-movies").append(item);
        });

        // Khởi tạo slider cho Phim Gợi Ý (Dàn hàng ngang và chạy)
        $('#list-recommended-movies').owlCarousel(getSliderSettings());
    }


    $('.tab-slider').owlCarousel({
        loop: true,
        responsiveClass: true,
        nav: false,
        dots: false,
        margin: 30,
        autoplay: true,
        autoplayTimeout: 2000,
        autoplayHoverPause: true,
        responsive: {
            0: {
                items: 1,
            },
            576: {
                items: 2,
            },
            768: {
                items: 2,
            },
            992: {
                items: 3,
            },
            1200: {
                items: 4,
            }
        }
    })

    // // 2. THÊM MỚI: Xử lý danh sách PHIM GỢI Ý
    // const isLogged = await TokenIsVaild();
    // let urlRecommendation = 'api/recommendations';

    // if (isLogged) {
    //     let uid = GetUserIdFromToken();
    //     if (uid) {
    //         urlRecommendation += `?userId=${uid}`;
    //     }
    // }

    // // Gọi API (Dùng isLogged cho tham số usingToken)
    // let recList = await PenguRequestAPI('GET', urlRecommendation, {}, {}, isLogged)
    //     .then(r => r.ok ? r.json() : []) // Check r.ok để tránh lỗi parse JSON khi server trả 403/500
    //     .catch(error => { console.log("Lỗi gợi ý:", error); return [] });

    // if (recList && recList.length > 0) {
    //     $("#recommendation-section").show(); 
    //     $("#list-recommended-movies").empty();

    //     recList.forEach(element => {
    //         let item = createMovieItem(element);
    //         $("#list-recommended-movies").append(item);
    //     });

    //     // Khởi tạo slider
    //     $('#list-recommended-movies').owlCarousel(getSliderSettings());
    // }
    // ============================================================

    
    

    $('.preloader').fadeOut(1000);
})


$(".ticket-search-form").on("submit", async function (event) {
    event.preventDefault();
    const form = $(this)
    let value = form.find("#moive-name").val()

    window.location.href = ('/movie-grid.html?search=' + encodeURI(value))
})

// --- Hàm hỗ trợ để tránh lặp code ---
function createMovieItem(element) {
    let item = $(`
        <div class="item">
            <div class="movie-grid">
                <div class="movie-thumb c-thumb">
                    <a href="movie-details.html?movie-id=${encodeURI(element.id)}">
                        <img src="${element.image}" alt="movie" style="width: 262px; height: 375px; object-fit: cover;">
                    </a>
                </div>
                <div class="movie-content bg-one">
                    <h5 class="title m-0"> 
                        <a href="movie-details.html?movie-id=${encodeURI(element.id)}">${element.title}</a>
                    </h5>
                    <ul class="movie-rating-percent">
                        <span class="content">Thời lượng: ${GetTimeString(element.durationInMins)}</span>
                        <br>
                        <span class="content">Thể loại: ${element.genres.map(d => d.genre).join(", ")}</span>
                    </ul>
                </div>
            </div> 
        </div>`
    );
    return item;
}

function getSliderSettings() {
    return {
        loop: true,
        responsiveClass: true,
        nav: false,
        dots: false,
        margin: 30,
        autoplay: true,
        autoplayTimeout: 2000,
        autoplayHoverPause: true,
        responsive: {
            0: { items: 1 },
            576: { items: 2 },
            768: { items: 2 },
            992: { items: 3 },
            1200: { items: 4 }
        }
    };
}