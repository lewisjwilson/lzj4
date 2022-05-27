
function lzo4_encoder()
  test_data = "abbccabbcccabbaabcc";
  data_len = length(test_data);
  
  # setting size of the search buffer (window) 
  window_buf = 19;
  pos = 1;
  
  # in the event that the window is larger than the data itself
  if(window_buf > data_len)
    window = substr(test_data, pos, data_len);
  else
    window = substr(test_data, pos, window_buf);
  end
  
  window
  
  # initialize values for searches
  sub_str = "";
  best_match = "";
  
  # for each value in the window
  for i = pos:window_buf
       
    cur_byte = test_data(i); # get the current window byte
    sub_str = strcat(sub_str, cur_byte); # append current byte onto substring for searching
    
    # matches much be >= 4
    if length(sub_str) < 5
      continue
    end
    
    sub_str
    matches = strfind(window, sub_str) # check if substring is in current window
    
    # if there are no matches
    if isempty(matches)
      # reset the substring and continue a search
      sub_str = "";
      continue
    else
      # if the length of the current best match is less than
      # the length of the current substring
      if length(best_match) < length(sub_str)
        #replace the best match
        best_match = sub_str;
      end
    end
    
  end
  
  best_match
  
  
endfunction
