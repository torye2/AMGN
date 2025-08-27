package amgn.amu.mapper;

import amgn.amu.dto.ListingDto;
import amgn.amu.entity.Listing;

public class ListingMapper {

    public static ListingDto toDto(Listing listing) {
        ListingDto dto = new ListingDto();
        dto.setListingId(listing.getListingId());
        dto.setSellerId(listing.getSellerId());
        dto.setTitle(listing.getTitle());
        dto.setPrice(listing.getPrice());
        dto.setNegotiable(listing.getNegotiable());
        dto.setCategoryId(listing.getCategoryId());
        dto.setItemCondition(listing.getItemCondition());
        dto.setDescription(listing.getDescription());
        dto.setTradeType(listing.getTradeType());
        dto.setRegionId(listing.getRegionId());
        dto.setSafePayYn(listing.getSafePayYn());
        return dto;
    }
}