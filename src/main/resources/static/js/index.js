  function execDaumPostcode() {
    new daum.Postcode({
      oncomplete: function(data) {
        
		var fullAddress = data.address
		
        document.getElementById('location-name').textContent = `📍${fullAddress}`;
		
		console.log("선택된 주소 : ", fullAddress);

      }
    }).open();
  }
