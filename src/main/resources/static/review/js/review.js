    // 로그인한 사용자 ID 예시 (raterId)
    const raterId = 123; 
    // 리뷰 조회할 사용자 ID (rateeId)
    const userId = 123; 
    const limit = 20;

    // 리뷰 리스트 불러오기 + 평균 평점 갱신
    async function loadReviews() {
        try {
            const res = await fetch(`/api/reviews/users/${userId}?limit=${limit}`);
            if (!res.ok) return;
            
            const reviews = await res.json();
            const listDiv = document.getElementById("review-list");
            listDiv.innerHTML = "";

            if (reviews.length === 0) {
                listDiv.innerHTML = "<p>리뷰가 없습니다.</p>";
                document.getElementById("ratingAverage").textContent = "-";
                return;
            }

            let sum = 0;
            reviews.forEach(r => {
                sum += r.score;
                const item = document.createElement("div");
                item.innerHTML = `
                    <p><strong>작성자 ID:</strong> ${r.raterId}</p>
                    <p><strong>평점:</strong> ${r.score}</p>
                    <p><strong>댓글:</strong> ${r.rvComment || "-"}</p>
                    <p><em>${new Date(r.createdAt).toLocaleString()}</em></p>
                    <hr>
                `;
                listDiv.appendChild(item);
            });

            // 평균 평점 갱신
            const avg = (sum / reviews.length).toFixed(1);
            document.getElementById("ratingAverage").textContent = avg;

        } catch (err) {
            alert(err.message);
        }
    }

    // 리뷰 작성
    document.getElementById("review-form").addEventListener("submit", async (e) => {
        e.preventDefault();

        const payload = {
            orderId: Number(document.getElementById("orderId").value),
            score: Number(document.getElementById("score").value),
            rvComment: document.getElementById("rvComment").value
        };

        try {
            const res = await fetch(`/api/reviews?uid=${raterId}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const errData = await res.json();
                throw new Error(errData.message || "리뷰 작성 중 오류 발생");
            }

            alert("리뷰 작성 완료!");
            document.getElementById("review-form").reset();
            loadReviews(); // 작성 후 리스트 + 평균 갱신
        } catch (err) {
            alert(err.message);
        }
    });

    window.addEventListener("DOMContentLoaded", loadReviews);
